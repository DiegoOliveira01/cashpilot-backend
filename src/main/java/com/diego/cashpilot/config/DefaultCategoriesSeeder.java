package com.diego.cashpilot.config;

import com.diego.cashpilot.model.Category;
import com.diego.cashpilot.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Garante as categorias padrão a cada subida. Idempotente: só insere o que ainda não existe. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultCategoriesSeeder implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(ApplicationArguments args) {
        int created = 0;

        for (DefaultCategories.Item item : DefaultCategories.ALL) {
            if (!categoryRepository.existsByUserIsNullAndNameIgnoreCaseAndType(item.name(), item.type())) {
                categoryRepository.save(Category.builder()
                        .name(item.name())
                        .type(item.type())
                        .icon(item.icon())
                        .build());
                created++;
            }
        }

        if (created > 0) {
            log.info("Categorias padrão criadas: {}", created);
        }
    }
}
