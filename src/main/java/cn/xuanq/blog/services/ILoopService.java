package cn.xuanq.blog.services;

import cn.xuanq.blog.pojo.Looper;
import cn.xuanq.blog.response.ResponseResult;

public interface ILoopService {
    ResponseResult addLoop(Looper looper);

    ResponseResult getLoop(String loopId);

    ResponseResult listLoops();

    ResponseResult updateLoop(String loopId, Looper looper);

    ResponseResult deleteLoop(String loopId);
}
