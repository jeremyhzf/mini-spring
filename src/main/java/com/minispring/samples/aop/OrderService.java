package com.minispring.samples.aop;

import com.minispring.stereotype.Service;

@Service
public class OrderService implements IOrderService {

    @Override
    public void createOrder(String orderNo) {
        System.out.println("   创建订单: " + orderNo);
    }

    @Override
    public void cancelOrder(String orderNo) {
        System.out.println("   取消订单: " + orderNo);
    }
}
