package com.minispring.condition.test;

import com.minispring.condition.ConditionalOnProperty;
import com.minispring.stereotype.Service;

/** 条件组件：仅当 feature.x=true 时注册 */
@Service
@ConditionalOnProperty(name = "feature.x", havingValue = "true")
public class FeatureEnabledService {
}
