package com.marketplace.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.api.entity.Commission;
import com.marketplace.api.entity.Order;
import com.marketplace.api.entity.OrderItem;
import com.marketplace.api.entity.User;
import com.marketplace.api.repository.CommissionRepository;
import com.marketplace.api.service.factory.CommissionStrategyFactory;

@Service
public class CommissionServiceImpl implements CommissionService {
    private final CommissionRepository commissionRepository;
    private final CommissionStrategyFactory commissionStrategyFactory;
    private final String commissionType;

    @Autowired
    public CommissionServiceImpl(CommissionRepository commissionRepository,
                                 CommissionStrategyFactory commissionStrategyFactory,
                                 @Value("${api.commission.type:STANDARD}") String commissionType) {
        this.commissionRepository = commissionRepository;
        this.commissionStrategyFactory = commissionStrategyFactory;
        this.commissionType = commissionType;
    }

    @Transactional
    public void saveCommissions(Order order) {
        Map<User, List<OrderItem>> itemsBySeller = order.getItems().stream()
                .collect(Collectors.groupingBy(item -> item.getProduct().getSeller()));

        BigDecimal totalCommission = commissionStrategyFactory.getStrategy(commissionType).calculate(order);
        BigDecimal orderSubtotal = order.getItems().stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (Map.Entry<User, List<OrderItem>> entry : itemsBySeller.entrySet()) {
            User seller = entry.getKey();
            List<OrderItem> sellerItems = entry.getValue();

            BigDecimal sellerSubtotal = sellerItems.stream()
                    .map(OrderItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal proportionalAmount = sellerSubtotal.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : totalCommission.multiply(sellerSubtotal).divide(orderSubtotal, RoundingMode.HALF_UP);

            Commission commission = Commission.builder()
                    .order(order)
                    .seller(seller)
                    .amount(proportionalAmount)
                    .build();

            commissionRepository.save(commission);
        }
    }

    public List<Commission> findBySeller(User seller) {
        return commissionRepository.findBySeller(seller);
    }
}
