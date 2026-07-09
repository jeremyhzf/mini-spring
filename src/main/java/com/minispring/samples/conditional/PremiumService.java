package com.minispring.samples.conditional;

import com.minispring.condition.ConditionalOnProperty;
import com.minispring.stereotype.Service;

/**
 * 高级服务：仅当 feature.premium=true 时注册
 */
@Service
@ConditionalOnProperty(name = "feature.premium", havingValue = "true")
public class PremiumService {

    public String serve() {
        return "高级服务";
    }
}
