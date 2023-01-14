package com.bzi.taskcloud.engine;

import com.bzi.taskcloud.common.lang.CommandType;
import com.bzi.taskcloud.common.lang.LogType;
import com.bzi.taskcloud.common.lang.TaskRunStatus;
import com.bzi.taskcloud.common.lang.TaskType;
import com.bzi.taskcloud.common.utils.LoggerUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

@Component
@ChannelHandler.Sharable
public class EngineTerminal extends ChannelInboundHandlerAdapter {
    private final String host;
    private final int port;
    private final String key;
    private final boolean reconnect;
    private final int reconnectInterval;
    private final String logRelativePath;

    private Bootstrap bootstrap;
    private Channel channel;
    private boolean isConnected = false;
    private final CompletableFuture<Object> waitForHandshake = new CompletableFuture<>();
    private final Map<CommandType, Queue<CompletableFuture<Object>>> futureResults = Map.of(
            CommandType.run, new ArrayDeque<>(),
            CommandType.stop, new ArrayDeque<>(),
            CommandType.status, new ArrayDeque<>()
    );
    private final Map<Long, FileWriter> userLogPool = new HashMap<>();
    private final Map<Long, TaskCompleteCallback> onTaskCompleteEventPool = new HashMap<>();

    public EngineTerminal(
        @Value("${engine.host}") String host,
        @Value("${engine.port}") int port,
        @Value("${engine.key}") String key,
        @Value("${engine.reconnect}") boolean reconnect,
        @Value("${engine.reconnectInterval}") int reconnectInterval,
        @Value("${engine.logRelativePath}") String logRelativePath
    ) throws ExecutionException, InterruptedException {
        this.host = host;
        this.port = port;
        this.key = key;
        this.reconnect = reconnect;
        this.reconnectInterval = reconnectInterval;
        this.logRelativePath = logRelativePath;

        for (int i = 0; i < 6; i++) {
            try {
                connectService();
                break;
            } catch (Exception e) {
                if (5 == i) {
                    System.out.println("==============================ERROR==============================");
                    LoggerUtil.failed("无法连接至目标脚本容器", e);
                    System.out.println("==============================ERROR==============================");
                    throw new Error();
                } else {
                    LoggerUtil.failed("第" + (i + 1) + "次尝试连接核心脚本容器失败，将在10秒后重试", e);
                    Thread.sleep(10000);
                }
            }
        }

        waitForHandshake.get();
        if (!isConnected) {
            System.out.println("==============================ERROR==============================");
            LoggerUtil.failed("与目标脚本容器握手失败，请检查KEY是否设置正确", new Throwable("target:" + host + ":" + port + " key:" + key));
            System.out.println("==============================ERROR==============================");
            throw new Error();
        } else {
            LoggerUtil.succeed("连接核心脚本容器成功", new Throwable("target:" + host + ":" + port));
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        handshake();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        isConnected = false;

        LoggerUtil.info("核心脚本容器已断开连接", new Throwable("target:" + host + ":" + port));

        // check if is a need to reconnect
        if (reconnect) {
            LoggerUtil.operation("开始尝试重连核心脚本容器", new Throwable("target:" + host + ":" + port));

            // start reconnect thread
            new Thread(() -> {
                while (!reconnectService()) {
                    LoggerUtil.failed("重连核心脚本容器失败，将在" + (reconnectInterval / 1000.f) + "秒后尝试", new Throwable("target:" + host + ":" + port));

                    try {
                        Thread.sleep(reconnectInterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                LoggerUtil.succeed("重连核心脚本容器成功", new Throwable("target:" + host + ":" + port));
            }).start();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof ByteBuf data))
            return;

        int packetSize = data.readInt();
        CommandType commandType = CommandType.values()[data.readByte()];
        switch (commandType) {
            case handshake -> {
                isConnected = data.readBoolean();
                waitForHandshake.complete(null);
            }
            case run -> {
                var future = futureResults.get(CommandType.run).poll();
                if (Objects.nonNull(future)) {
                    if (data.readBoolean())
                        future.complete(data.readLong());
                    else
                        future.complete(0L);
                }
            }
            case stop -> {
                var future = futureResults.get(CommandType.stop).poll();
                if (Objects.nonNull(future))
                    future.complete(data.readBoolean());
            }
            case status -> {
                var future = futureResults.get(CommandType.status).poll();
                if (Objects.nonNull(future))
                    future.complete(TaskRunStatus.values()[data.readByte()]);
            }
            case log -> {
                var userId = data.readLong();
                var taskId = data.readLong();
                var logType = LogType.values()[data.readByte()];
                var taskName = new byte[data.readInt()];
                data.readBytes(taskName);
                var dataLog = new byte[data.readInt()];
                data.readBytes(dataLog);

                if (!userLogPool.containsKey(userId)) {
                    File logPath = new File(System.getProperty("user.dir") + logRelativePath);
                    File userLogPath = new File(System.getProperty("user.dir") + logRelativePath + userId);
                    File userLogFile = new File(userLogPath + "/output.log");

                    if (!logPath.exists()) {
                        if (!logPath.mkdir()) {
                            LoggerUtil.failed("创建日志目录失败", new Throwable());
                            break;
                        }
                    }
                    if (!userLogPath.exists()) {
                        if (!userLogPath.mkdir()) {
                            LoggerUtil.failed("创建用户日志目录失败", new Throwable());
                            break;
                        }
                    }

                    userLogPool.put(userId, new FileWriter(userLogFile, true));
                }

                var logger = userLogPool.get(userId);
                switch (logType) {
                    case operation -> logger.write("[*] ");
                    case info -> logger.write("[=] ");
                    case failed -> logger.write("[-] ");
                    case succeed -> logger.write("[+] ");
                }
                logger.write(String.format(
                        "[%s][%d:%s] -> %s",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()),
                        taskId,
                        new String(taskName),
                        new String(dataLog)
                ));
                logger.flush();
            }
            case result -> {
                var userId = data.readLong();
                var taskId = data.readLong();
                var result = data.readBoolean();
                var runnerId = data.readLong();

                if (onTaskCompleteEventPool.containsKey(runnerId)) {
                    onTaskCompleteEventPool.get(runnerId).run(result);
                    onTaskCompleteEventPool.remove(runnerId);
                }
            }
        }
    }

    private void connectService() throws InterruptedException {
        bootstrap = new Bootstrap();

        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.group(new NioEventLoopGroup());
        bootstrap.remoteAddress(host, port);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline().addFirst(new LengthFieldBasedFrameDecoder(
                        10 * 1024 * 1024,
                        0,
                        4,
                        0,
                        0
                ));

                socketChannel.pipeline().addLast(EngineTerminal.this);
            }
        });

        channel = bootstrap.connect().sync().channel();
    }

    private boolean reconnectService() {
        try {
            channel = bootstrap.connect().sync().channel();
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private void handshake() {
        ByteBuf data = Unpooled.buffer();

        byte[] dataKey = key.getBytes();

        data.writeInt(9 + dataKey.length);
        data.writeByte(CommandType.handshake.ordinal());
        data.writeInt(dataKey.length);
        data.writeBytes(dataKey);

        channel.writeAndFlush(data);
    }

    public Long run(Long userId, Long taskId, TaskType taskType, String taskName, String taskScript, String passport, String interfaces) {
        if (!isConnected)
            return 0L;
        ByteBuf data = Unpooled.buffer();

        byte[] dataTaskName = taskName.getBytes();
        byte[] dataTaskScript = taskScript.getBytes();
        byte[] dataPassport = passport.getBytes();
        byte[] dataInterfaces = interfaces.getBytes();

        data.writeInt(
                26
                + dataTaskName.length
                + dataTaskScript.length
                + dataPassport.length
                + dataInterfaces.length
        );
        data.writeByte(CommandType.run.ordinal());
        data.writeLong(userId);
        data.writeLong(taskId);
        data.writeByte(taskType.ordinal());
        data.writeInt(dataTaskName.length);
        data.writeBytes(dataTaskName);
        data.writeInt(dataTaskScript.length);
        data.writeBytes(dataTaskScript);
        data.writeInt(dataPassport.length);
        data.writeBytes(dataPassport);
        data.writeInt(dataInterfaces.length);
        data.writeBytes(dataInterfaces);

        var result = new CompletableFuture<>();
        futureResults.get(CommandType.run).add(result);

        channel.writeAndFlush(data);
        try {
            return (Long) result.get();
        } catch (InterruptedException | ExecutionException e) {
            LoggerUtil.failed(e);
            return 0L;
        }
    }

    public Boolean stop(Long runnerId) {
        if (!isConnected)
            return false;
        ByteBuf data = Unpooled.buffer();

        data.writeInt(13);
        data.writeByte(CommandType.stop.ordinal());
        data.writeLong(runnerId);

        var result = new CompletableFuture<>();
        futureResults.get(CommandType.stop).add(result);

        channel.writeAndFlush(data);
        try {
            return (Boolean) result.get();
        } catch (InterruptedException | ExecutionException e) {
            LoggerUtil.failed(e);
            return false;
        }
    }

    public TaskRunStatus status(Long runnerId) {
        if (!isConnected)
            return TaskRunStatus.none;
        ByteBuf data = Unpooled.buffer();

        data.writeInt(13);
        data.writeByte(CommandType.status.ordinal());
        data.writeLong(runnerId);

        var result = new CompletableFuture<>();
        futureResults.get(CommandType.status).add(result);

        channel.writeAndFlush(data);
        try {
            return (TaskRunStatus) result.get();
        } catch (InterruptedException | ExecutionException e) {
            LoggerUtil.failed(e);
            return TaskRunStatus.none;
        }
    }

    public void onTaskComplete(Long runnerId, TaskCompleteCallback callback) {
        onTaskCompleteEventPool.put(runnerId, callback);
    }
}
