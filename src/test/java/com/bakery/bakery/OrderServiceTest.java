
package com.bakery.bakery.service;

import com.bakery.bakery.dto.CreateOrderDTO;
import com.bakery.bakery.model.Order;
import com.bakery.bakery.model.User;
import com.bakery.bakery.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@bakery.com");
    }

    // ── createOrder ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("createOrder: зберігає замовлення зі статусом NEW")
    void createOrder_savesWithStatusNew() {
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setComposition("Шоколадний торт");
        dto.setAddress("вул. Шевченка 1");
        dto.setDeadline("2025-12-31T12:00");

        Order savedOrder = new Order();
        savedOrder.setId(42L);
        savedOrder.setOrderStatus(Order.OrderStatus.NEW);
        when(orderRepository.save(any())).thenReturn(savedOrder);

        Order result = orderService.createOrder(dto, testUser);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getOrderStatus()).isEqualTo(Order.OrderStatus.NEW);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    // ── cancelByUser ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelByUser: змінює статус на CANCELLED для NEW замовлення")
    void cancelByUser_setsStatusCancelled() {
        Order order = new Order();
        order.setId(1L);
        order.setUser(testUser);
        order.setOrderStatus(Order.OrderStatus.NEW);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.cancelByUser(1L, testUser.getId());

        assertThat(order.getOrderStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("cancelByUser: кидає IllegalStateException якщо замовлення вже IN_PROGRESS")
    void cancelByUser_throwsIfNotNew() {
        Order order = new Order();
        order.setId(2L);
        order.setUser(testUser);
        order.setOrderStatus(Order.OrderStatus.IN_PROGRESS);

        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelByUser(2L, testUser.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("нові");
    }

    @Test
    @DisplayName("cancelByUser: кидає SecurityException якщо юзер не власник")
    void cancelByUser_throwsIfWrongUser() {
        User anotherUser = new User();
        anotherUser.setId(99L);

        Order order = new Order();
        order.setId(3L);
        order.setUser(anotherUser);
        order.setOrderStatus(Order.OrderStatus.NEW);

        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelByUser(3L, testUser.getId()))
                .isInstanceOf(SecurityException.class);
    }

    // ── findByIdOrThrow ───────────────────────────────────────────────────────

    @Test
    @DisplayName("findByIdOrThrow: кидає IllegalArgumentException якщо не знайдено")
    void findByIdOrThrow_throwsIfNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findByIdOrThrow(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }
}
