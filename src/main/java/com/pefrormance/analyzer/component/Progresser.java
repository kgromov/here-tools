package com.pefrormance.analyzer.component;

import com.pefrormance.analyzer.config.InjectedSettings;
import javafx.concurrent.Task;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by konstantin on 28.04.2020.
 */
@Component
public class Progresser extends Task<Void> {
    private static final NumberFormat FORMAT = NumberFormat.getPercentInstance();
    private final AtomicInteger progress;
    private final int productsAmount;

    public Progresser(InjectedSettings settings) {
        this.productsAmount = settings.getProducts().size();
        this.progress = new AtomicInteger();
    }

    public void init() {
        updateProgress(progress.get(), productsAmount);
        updateMessage("Progress: " + FORMAT.format(progress.doubleValue() / productsAmount));
    }

    public void increment() {
        progress.incrementAndGet();
        updateProgress(progress.get(), productsAmount);
        updateMessage("Progress: " + FORMAT.format(progress.doubleValue() / productsAmount));
    }

    @Override
    protected Void call() throws Exception {
        // nothing to do - is used for rendering progress bar and label
        return null;
    }
}
