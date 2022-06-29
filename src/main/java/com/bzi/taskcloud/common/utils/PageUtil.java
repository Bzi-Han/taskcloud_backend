package com.bzi.taskcloud.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bzi.taskcloud.common.vo.PageVO;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class PageUtil {
    public static <V, T> PageVO<V> filterResult(IPage<T> dataPage, Class<V> VOClazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        PageVO<V> pageVO = new PageVO<>();
        List<V> records = new ArrayList<>();

        BeanUtils.copyProperties(dataPage, pageVO, "records");
        for (T data : dataPage.getRecords()) {
            V recordVO = VOClazz.getDeclaredConstructor().newInstance();

            BeanUtils.copyProperties(data, recordVO);

            records.add(recordVO);
        }
        pageVO.setRecords(records);

        return pageVO;
    }

    public static <T> PageVO<T> filterResult(IPage<T> dataPage) {
        PageVO<T> pageVO = new PageVO<>();

        BeanUtils.copyProperties(dataPage, pageVO);

        return pageVO;
    }
}
