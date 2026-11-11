package com.diego.cashpilot.service;

import com.diego.cashpilot.model.CreditCard;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class CreditCardCalculator {

    public record InstallmentInfo(BigDecimal amount, LocalDate date, int number, int total) {}

    public List<InstallmentInfo> calculateInstallments(CreditCard card, LocalDate purchaseDate, BigDecimal totalAmount, int installments) {
        List<InstallmentInfo> result = new ArrayList<>();

        // 1. Lógica para descobrir a primeira fatura
        // Se a compra foi feita depois do dia de fechamento, cai no mês seguinte
        LocalDate invoiceBaseDate = purchaseDate;
        if (purchaseDate.getDayOfMonth() > card.getClosingDay()) {
            invoiceBaseDate = purchaseDate.plusMonths(1);
        }

        // 2. Divisão dos valores (evitando centavos perdidos)
        BigDecimal baseValue = totalAmount.divide(BigDecimal.valueOf(installments), 2, RoundingMode.DOWN);
        BigDecimal remainder = totalAmount.subtract(baseValue.multiply(BigDecimal.valueOf(installments)));

        for (int i = 0; i < installments; i++) {
            BigDecimal currentAmount = baseValue;

            // A última parcela absorve a diferença dos centavos (ex: R$ 100 / 3 = 33.33, 33.33 e 33.34)
            if (i == installments - 1) {
                currentAmount = currentAmount.add(remainder);
            }

            // 3. Calcula a data da parcela (mês base + i meses)
            LocalDate installmentDate = invoiceBaseDate.plusMonths(i);
            // Ajusta o dia para o dia de vencimento do cartão
            installmentDate = installmentDate.withDayOfMonth(
                    Math.min(card.getDueDay(), installmentDate.lengthOfMonth())
            );

            result.add(new InstallmentInfo(currentAmount, installmentDate, i + 1, installments));
        }

        return result;
    }
}