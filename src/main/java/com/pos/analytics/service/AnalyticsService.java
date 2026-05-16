package com.pos.analytics.service;

import com.pos.analytics.dto.DashboardResponse;
import com.pos.analytics.dto.TopProductResponse;
import com.pos.product.dto.ProductResponse;

import java.util.List;

public interface AnalyticsService {

    DashboardResponse getDashboardData();

    List<ProductResponse> getLowStockProducts();

    List<TopProductResponse> getTopSellingProducts();
}