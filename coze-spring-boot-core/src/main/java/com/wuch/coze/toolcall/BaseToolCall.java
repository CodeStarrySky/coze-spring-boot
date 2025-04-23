package com.wuch.coze.toolcall;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.wuch.coze.api.AiResult;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.BiFunction;

public abstract class BaseToolCall<T, R> implements BiFunction<String, Map<String, String>, String> {

    @Override
    public String apply(String data, Map<String, String> metaData) {
        T t = JSONObject.parseObject(data, getRequestClass());
        R resp = call(t, metaData);
        if (resp instanceof String str) {
            return str;
        }
        return JSONObject.toJSONString(resp);
    }

    /**
     * execute 方法只需要重写一个即可，
     * @param t 请求参数
     * @param metaData 元数据
     * @return 执行结果
     */
    protected R call(T t, Map<String, String> metaData) {
        return call(t);
    }

    protected R call(T t) {
        return null;
    }

    @SuppressWarnings("unchecked")
    private Class<T> getRequestClass(){
        Type type = this.getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length > 0) {
                return (Class<T>) actualTypeArguments[0];
            }
        }
        throw new RuntimeException("没有获取到Request的类型，请加泛型");
    }

}
