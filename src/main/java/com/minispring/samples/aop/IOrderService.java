package com.minispring.samples.aop;

/**
 * 订单服务接口
 */
public interface IOrderService {

    void createOrder(String orderNo);

    void cancelOrder(String orderNo);
}
