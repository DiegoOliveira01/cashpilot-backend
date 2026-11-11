package com.diego.cashpilot.service;

import com.diego.cashpilot.model.CreditCard;
import com.diego.cashpilot.model.CreditCardInvoice;
import com.diego.cashpilot.model.Transaction;
import com.diego.cashpilot.repository.CreditCardInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class CreditCardInvoiceService {

    private final CreditCardInvoiceRepository invoiceRepository;

    /**
     * Busca ou cria a fatura correspondente à data da compra.
     */
    @Transactional
    public CreditCardInvoice getOrCreateInvoice(CreditCard card, LocalDate purchaseDate) {
        // Lógica para definir o mês da fatura baseado no fechamento
        LocalDate invoiceBaseDate = purchaseDate;
        if (purchaseDate.getDayOfMonth() > card.getClosingDay()) {
            invoiceBaseDate = purchaseDate.plusMonths(1);
        }

        String refMonth = YearMonth.from(invoiceBaseDate).format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // Tenta achar a fatura existente
        return invoiceRepository.findByCreditCardIdAndReferenceMonth(card.getId(), refMonth)
                .orElseGet(() -> {
                    // Se não achar, cria uma nova
                    CreditCardInvoice newInvoice = new CreditCardInvoice();
                    newInvoice.setCreditCard(card);
                    newInvoice.setReferenceMonth(refMonth);

                    // Calcula data de fechamento (mês da fatura, dia de fechamento)
                    newInvoice.setClosingDate(invoiceBaseDate.withDayOfMonth(
                            Math.min(card.getClosingDay(), invoiceBaseDate.lengthOfMonth())
                    ));

                    // Calcula data de vencimento (geralmente no mês seguinte ao fechamento)
                    LocalDate dueDate = invoiceBaseDate.plusMonths(1).withDayOfMonth(
                            Math.min(card.getDueDay(), invoiceBaseDate.plusMonths(1).lengthOfMonth())
                    );
                    newInvoice.setDueDate(dueDate);

                    newInvoice.setTotalAmount(BigDecimal.ZERO);
                    newInvoice.setIsManual(false);
                    newInvoice.setIsClosed(false);
                    newInvoice.setIsPaid(false);

                    return invoiceRepository.save(newInvoice);
                });
    }

    /**
     * Atualiza o valor total da fatura após uma compra ser adicionada.
     */
    @Transactional
    public void addTransactionToInvoice(CreditCardInvoice invoice, BigDecimal amount) {
        invoice.setTotalAmount(invoice.getTotalAmount().add(amount));
        invoiceRepository.save(invoice);
    }
}