package com.foodorderingsystem.payment;

import com.foodorderingsystem.customer.entity.Customer;
import com.foodorderingsystem.customer.repository.CustomerRepository;
import com.foodorderingsystem.delivery.repository.DeliveryRepository;
import com.foodorderingsystem.order.entity.Order;
import com.foodorderingsystem.order.repository.OrderRepository;
import com.foodorderingsystem.payment.repository.PaymentRepository;
import com.foodorderingsystem.payment.repository.ReceiptRepository;
import com.foodorderingsystem.payment.repository.InvoiceRepository;
import com.foodorderingsystem.payment.repository.RefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PaymentControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ReceiptRepository receiptRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private RefundRepository refundRepository;
    @Autowired private DeliveryRepository deliveryRepository;

    private Customer ownerCustomer;
    private Customer otherCustomer;
    private Order ownerOrder;

    @BeforeEach
    void setup() {
        deliveryRepository.deleteAll();
        refundRepository.deleteAll();
        invoiceRepository.deleteAll();
        receiptRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        customerRepository.deleteAll();

        ownerCustomer = new Customer();
        ownerCustomer.setName("Owner User");
        ownerCustomer.setEmail("owner@test.com");
        ownerCustomer.setPassword("pass123");
        ownerCustomer.setPhone("0711111111");
        ownerCustomer.setAddress("Owner Address");
        ownerCustomer = customerRepository.save(ownerCustomer);

        otherCustomer = new Customer();
        otherCustomer.setName("Other User");
        otherCustomer.setEmail("other@test.com");
        otherCustomer.setPassword("pass123");
        otherCustomer.setPhone("0722222222");
        otherCustomer.setAddress("Other Address");
        otherCustomer = customerRepository.save(otherCustomer);

        ownerOrder = new Order();
        ownerOrder.setCustomer(ownerCustomer);
        ownerOrder.setDeliveryAddress("Owner Address");
        ownerOrder.setTotalAmount(1950.00);
        ownerOrder.setStatus("PENDING");
        ownerOrder.setPaymentStatus("UNPAID");
        ownerOrder = orderRepository.save(ownerOrder);
    }

    @Test
    void unauthenticatedUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/payment/checkout/" + ownerOrder.getOrderId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void otherCustomerCannotAccessCheckout() throws Exception {
        MockHttpSession otherSession = new MockHttpSession();
        otherSession.setAttribute("loggedInCustomer", otherCustomer);

        mockMvc.perform(get("/payment/checkout/" + ownerOrder.getOrderId()).session(otherSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/dashboard"));
    }

    @Test
    void orderOwnerCanAccessCheckout() throws Exception {
        MockHttpSession ownerSession = new MockHttpSession();
        ownerSession.setAttribute("loggedInCustomer", ownerCustomer);

        mockMvc.perform(get("/payment/checkout/" + ownerOrder.getOrderId()).session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(view().name("payment/checkout"))
                .andExpect(model().attributeExists("order", "paymentRequest", "paymentMethods"));
    }

    @Test
    void otherCustomerCannotPayOwnerOrder() throws Exception {
        MockHttpSession otherSession = new MockHttpSession();
        otherSession.setAttribute("loggedInCustomer", otherCustomer);

        mockMvc.perform(post("/payment/pay")
                        .session(otherSession)
                        .param("orderId", ownerOrder.getOrderId().toString())
                        .param("paymentMethod", "CARD")
                        .param("testPaymentDetails", "4111 1111 1111 1111"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/dashboard"));
    }

    @Test
    void ownerPaymentSuccessRedirectsToInvoice() throws Exception {
        MockHttpSession ownerSession = new MockHttpSession();
        ownerSession.setAttribute("loggedInCustomer", ownerCustomer);

        mockMvc.perform(post("/payment/pay")
                        .session(ownerSession)
                        .param("orderId", ownerOrder.getOrderId().toString())
                        .param("paymentMethod", "CARD")
                        .param("testPaymentDetails", "4111 1111 1111 1111"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payment/invoice/" + ownerOrder.getOrderId()));
    }

    @Test
    void ownerPaymentDeclineRendersFailureView() throws Exception {
        MockHttpSession ownerSession = new MockHttpSession();
        ownerSession.setAttribute("loggedInCustomer", ownerCustomer);

        mockMvc.perform(post("/payment/pay")
                        .session(ownerSession)
                        .param("orderId", ownerOrder.getOrderId().toString())
                        .param("paymentMethod", "CARD")
                        .param("testPaymentDetails", "DECLINE"))
                .andExpect(status().isOk())
                .andExpect(view().name("payment/failure"))
                .andExpect(model().attributeExists("order", "payment"));
    }

    @Test
    void adminCanAccessReportsAndExport() throws Exception {
        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute("isAdmin", true);

        mockMvc.perform(get("/payment/reports").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("payment/reports"));

        mockMvc.perform(get("/payment/reports/export").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"sales_report.csv\""));
    }
}
