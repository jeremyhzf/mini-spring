package com.minispring.samples.conditional;

import com.minispring.stereotype.Service;

/**
 * 常驻服务：无条件，扫描时恒注册
 */
@Service
public class BasicService {

    public String serve() {
        return "基础服务";
    }
}
