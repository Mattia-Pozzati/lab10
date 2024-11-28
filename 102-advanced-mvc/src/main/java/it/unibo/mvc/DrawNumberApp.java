package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    private static final int MIN = 0;
    private static final int MAX = 100;
    private static final int ATTEMPTS = 10;
    private static final int KEY = 0;
    private static final int VALUE = 1;

    private final DrawNumber model;
    private final List<DrawNumberView> views;
    private Configuration config;

    /**
     * @param views
     *            the views to attach
     */
    public DrawNumberApp(final String configFile, final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }

        this.config = getConfig(configFile);
        if(config.isConsistent()) {
            this.model = new DrawNumberImpl(config);
        } else {
            this.model = new DrawNumberImpl(new Configuration.Builder().build(MIN, MAX, ATTEMPTS));
            displayError(config.toString());
        }
        
        
    }

    private void displayError(final String msg) {
        for(var view : views) {
            view.displayError(msg);
        }
        quit();
    }

    public Map<String, Integer> parseConfigFile(final String configFile) {
        Map<String, Integer> config = new HashMap<>();
        try (final BufferedReader bufferReader = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(configFile)))) {
            for(String line = bufferReader.readLine(); line != null; line = bufferReader.readLine()) {
                final String[] splittedLine = line.split(":");
                final String key = splittedLine[KEY];
                final int val = Integer.parseInt(splittedLine[VALUE].trim());
                config.put(key, val);
            }
        } catch (IOException e) {
            displayError("File non trovato");
        } catch (NumberFormatException e) {
            displayError("Problema nella formattazione del file config");
        }
        return config;
    }

    private Configuration getConfig(final String configFilePath) {
        final Map<String, Integer> config = parseConfigFile(configFilePath);
        System.out.println(config);
        final int min = Optional.of(config.get("minimum")).orElse(MIN);
        final int max = Optional.of(config.get("maximum")).orElse(MAX);
        final int attempts = Optional.of(config.get("attempts")).orElse(ATTEMPTS);
        return new Configuration.Builder().build(min, max, attempts);
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view: views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view: views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * @param args
     *            ignored
     * @throws FileNotFoundException 
     */
    public static void main(final String... args) throws FileNotFoundException {
        new DrawNumberApp("config.yml", 
            
        new DrawNumberViewImpl(),
            new DrawNumberViewImpl(),
            new PrintStreamView(System.out),
            new PrintStreamView("output.log"));
    }

}
