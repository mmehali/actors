package com.offbynull.peernetic.gateways.visualizer;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.lang3.Validate;

public final class GraphApplication extends Application {

    private static volatile GraphApplication instance;
    private static final Lock lock = new ReentrantLock();
    private static final Condition startedCondition = lock.newCondition();
    private static final Condition stoppedCondition = lock.newCondition();
    
    private ListView<String> listView;
    private ObservableList<String> listItems;
    private Map<String, GraphStage> graphStages;

    @Override
    public void init() throws Exception {
        Platform.setImplicitExit(true);
        graphStages = new HashMap<>();
    }

    @Override
    public void start(Stage stage) {
        listItems = FXCollections.observableArrayList();
        listView = new ListView<>(listItems);
        
        listView.setOnMouseClicked((MouseEvent click) -> {
            if (click.getClickCount() == 2) {
                String name = listView.getSelectionModel().getSelectedItem();
                graphStages.get(name).show();
            }
        });
        
        stage.setTitle("Graph Selection");
        stage.setWidth(150);
        stage.setHeight(400);
        
        // We have to do this because in some cases JavaFX threads won't shut down when the last stage closes, even if implicitExit is set
        // http://stackoverflow.com/questions/15808063/how-to-stop-javafx-application-thread/22997736#22997736
        stage.setOnCloseRequest(x -> Platform.exit());

        Scene scene = new Scene(listView, 1.0, 1.0, true, SceneAntialiasing.DISABLED);
        scene.setFill(Color.WHITESMOKE);
        stage.setScene(scene);
        
        stage.show();
        
        lock.lock();
        try {
            instance = this;
            startedCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void stop() throws Exception {
        lock.lock();
        try {
            instance = null;
            stoppedCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public static GraphApplication getInstance() {
        lock.lock();
        try {
            return instance;
        } finally {
            lock.unlock();
        }
    }
    
    public static GraphApplication awaitStarted() throws InterruptedException {
        lock.lock();
        try {
            if (instance == null) {
                startedCondition.await();
            }
            return instance;
        } finally {
            lock.unlock();
        }
    }

    public static void awaitStopped() throws InterruptedException {
        lock.lock();
        try {
            if (instance != null) {
                stoppedCondition.await();
            }
        } finally {
            lock.unlock();
        }
    }
    
    void execute(MultiMap<String, Object> commands) {
        Validate.notNull(commands);
        
        Platform.runLater(() -> {
            for (Entry<String, Object> entry : commands.entrySet()) {
                String fromAddress = entry.getKey();
                GraphStage graphStage = graphStages.get(fromAddress);
                if (graphStage == null) {
                    graphStage = new GraphStage();
                    graphStages.put(fromAddress, graphStage);
                    listItems.add(fromAddress);
                    
                    if (listItems.size() == 1) { // if this is the first graph, show it
                        graphStage.show();
                    }
                }

                @SuppressWarnings("unchecked")
                Collection<Object> stageCommands = (Collection<Object>) entry.getValue();

                List<Runnable> stageRunnables = graphStage.execute(stageCommands);
                for (Runnable runnable : stageRunnables) {
                    try {
                        runnable.run();
                    } catch (RuntimeException re) {
                        // TODO log here
                    }
                }
            }
        });
    }
}