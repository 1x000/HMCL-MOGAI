/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2021  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.CU.ui.decorator;

import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXSnackbar;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.jackhuang.CU.Launcher;
import org.jackhuang.CU.auth.authlibinjector.AuthlibInjectorDnD;
import org.jackhuang.CU.setting.EnumBackgroundImage;
import org.jackhuang.CU.task.Schedulers;
import org.jackhuang.CU.ui.Controllers;
import org.jackhuang.CU.ui.FXUtils;
import org.jackhuang.CU.ui.account.AddAuthlibInjectorServerPane;
import org.jackhuang.CU.ui.animation.ContainerAnimations;
import org.jackhuang.CU.ui.construct.DialogAware;
import org.jackhuang.CU.ui.construct.DialogCloseEvent;
import org.jackhuang.CU.ui.construct.Navigator;
import org.jackhuang.CU.ui.construct.StackContainerPane;
import org.jackhuang.CU.ui.wizard.Refreshable;
import org.jackhuang.CU.ui.wizard.WizardProvider;
import org.jackhuang.CU.util.io.NetworkUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Stream;

import static java.util.logging.Level.WARNING;
import static java.util.stream.Collectors.toList;
import static org.jackhuang.CU.setting.ConfigHolder.config;
import static org.jackhuang.CU.ui.FXUtils.newImage;
import static org.jackhuang.CU.ui.FXUtils.onEscPressed;
import static org.jackhuang.CU.util.Logging.LOG;
import static org.jackhuang.CU.util.io.FileUtils.getExtension;

public class DecoratorController {
    private static final String PROPERTY_DIALOG_CLOSE_HANDLER = DecoratorController.class.getName() + ".dialog.closeListener";

    private final Decorator decorator;
    private final Navigator navigator;

    private JFXDialog dialog;
    private StackContainerPane dialogPane;

    public DecoratorController(Stage stage, Node mainPage) {
        decorator = new Decorator(stage);
        decorator.setOnCloseButtonAction(Launcher::stopApplication);
        decorator.titleTransparentProperty().bind(config().titleTransparentProperty());

        navigator = new Navigator();
        navigator.setOnNavigated(this::onNavigated);
        navigator.init(mainPage);

        decorator.getContent().setAll(navigator);
        decorator.onCloseNavButtonActionProperty().set(e -> close());
        decorator.onBackNavButtonActionProperty().set(e -> back());
        decorator.onRefreshNavButtonActionProperty().set(e -> refresh());

        setupAuthlibInjectorDnD();

        // Setup background
        decorator.setContentBackground(getBackground());
        changeBackgroundListener = o -> {
            final int currentCount = ++this.changeBackgroundCount;
            CompletableFuture.supplyAsync(this::getBackground, Schedulers.io())
                    .thenAcceptAsync(background -> {
                        if (this.changeBackgroundCount == currentCount)
                            decorator.setContentBackground(background);
                    }, Schedulers.javafx());
        };
        WeakInvalidationListener weakListener = new WeakInvalidationListener(changeBackgroundListener);
        config().backgroundImageTypeProperty().addListener(weakListener);
        config().backgroundImageProperty().addListener(weakListener);
        config().backgroundImageUrlProperty().addListener(weakListener);

        // pass key events to current dialog / current page
        decorator.addEventFilter(KeyEvent.ANY, e -> {
            if (!(e.getTarget() instanceof Node)) {
                return; // event source can't be determined
            }

            Node newTarget;
            if (dialogPane != null && dialogPane.peek().isPresent()) {
                newTarget = dialogPane.peek().get(); // current dialog
            } else {
                newTarget = navigator.getCurrentPage(); // current page
            }

            boolean needsRedirect = true;
            Node t = (Node) e.getTarget();
            while (t != null) {
                if (t == newTarget) {
                    // current event target is in newTarget
                    needsRedirect = false;
                    break;
                }
                t = t.getParent();
            }
            if (!needsRedirect) {
                return;
            }

            e.consume();
            newTarget.fireEvent(e.copyFor(e.getSource(), newTarget));
        });

        // press ESC to go back
        onEscPressed(navigator, this::back);
    }

    public Decorator getDecorator() {
        return decorator;
    }

    // ==== Background ====

    //FXThread
    private int changeBackgroundCount = 0;

    @SuppressWarnings("FieldCanBeLocal") // Strong reference
    private final InvalidationListener changeBackgroundListener;

    private Background getBackground() {
        EnumBackgroundImage imageType = config().getBackgroundImageType();

        Image image = null;
        switch (imageType) {
            case CUSTOM:
                String backgroundImage = config().getBackgroundImage();
                if (backgroundImage != null)
                    image = tryLoadImage(Paths.get(backgroundImage)).orElse(null);
                break;
            case NETWORK:
                String backgroundImageUrl = config().getBackgroundImageUrl();
                if (backgroundImageUrl != null && NetworkUtils.isURL(backgroundImageUrl))
                    image = tryLoadImage(backgroundImageUrl).orElse(null);
                break;
            case CLASSIC:
                image = newImage("/assets/img/background-classic.jpg");
                break;
            case TRANSLUCENT:
                return new Background(new BackgroundFill(new Color(1, 1, 1, 0.5), CornerRadii.EMPTY, Insets.EMPTY));
        }
        if (image == null) {
            image = loadDefaultBackgroundImage();
        }
        return new Background(new BackgroundImage(image, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, new BackgroundSize(800, 480, false, false, true, true)));
    }

    private volatile Image defaultBackground;

    /**
     * Load background image from bg/, background.png, background.jpg, background.gif
     */
    private Image loadDefaultBackgroundImage() {
        Optional<Image> image = randomImageIn(Paths.get("bg"));
        if (!image.isPresent()) {
            image = tryLoadImage(Paths.get("background.png"));
        }
        if (!image.isPresent()) {
            image = tryLoadImage(Paths.get("background.jpg"));
        }
        if (!image.isPresent()) {
            image = tryLoadImage(Paths.get("background.gif"));
        }

        return image.orElseGet(() -> {
            if (defaultBackground == null)
                defaultBackground = newImage("/assets/img/background.jpg");
            return defaultBackground;
        });
    }

    private Optional<Image> randomImageIn(Path imageDir) {
        if (!Files.isDirectory(imageDir)) {
            return Optional.empty();
        }

        List<Path> candidates;
        try (Stream<Path> stream = Files.list(imageDir)) {
            candidates = stream
                    .filter(Files::isReadable)
                    .filter(it -> {
                        String ext = getExtension(it).toLowerCase(Locale.ROOT);
                        return ext.equals("png") || ext.equals("jpg") || ext.equals("gif");
                    })
                    .collect(toList());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to list files in ./bg", e);
            return Optional.empty();
        }

        Random rnd = new Random();
        while (candidates.size() > 0) {
            int selected = rnd.nextInt(candidates.size());
            Optional<Image> loaded = tryLoadImage(candidates.get(selected));
            if (loaded.isPresent()) {
                return loaded;
            } else {
                candidates.remove(selected);
            }
        }
        return Optional.empty();
    }

    private Optional<Image> tryLoadImage(Path path) {
        if (!Files.isReadable(path))
            return Optional.empty();

        return tryLoadImage(path.toAbsolutePath().toUri().toString());
    }

    private Optional<Image> tryLoadImage(String url) {
        Image img;
        try {
            img = new Image(url);
        } catch (IllegalArgumentException e) {
            LOG.log(WARNING, "Couldn't load background image", e);
            return Optional.empty();
        }

        if (img.getException() != null) {
            LOG.log(WARNING, "Couldn't load background image", img.getException());
            return Optional.empty();
        }

        return Optional.of(img);
    }

    // ==== Navigation ====

    private static final DecoratorAnimationProducer animation = new DecoratorAnimationProducer();

    public void navigate(Node node) {
        navigator.navigate(node, animation);
    }

    private void close() {
        if (navigator.getCurrentPage() instanceof DecoratorPage) {
            DecoratorPage page = (DecoratorPage) navigator.getCurrentPage();

            if (page.isPageCloseable()) {
                page.closePage();
                return;
            }
        }
        navigator.clear();
    }

    private void back() {
        if (navigator.getCurrentPage() instanceof DecoratorPage) {
            DecoratorPage page = (DecoratorPage) navigator.getCurrentPage();

            if (page.back()) {
                if (navigator.canGoBack()) {
                    navigator.close();
                }
            }
        } else {
            if (navigator.canGoBack()) {
                navigator.close();
            }
        }
    }

    private void refresh() {
        if (navigator.getCurrentPage() instanceof Refreshable) {
            Refreshable refreshable = (Refreshable) navigator.getCurrentPage();

            if (refreshable.refreshableProperty().get())
                refreshable.refresh();
        }
    }

    private void onNavigated(Navigator.NavigationEvent event) {
        if (event.getSource() != this.navigator) return;
        Node to = event.getNode();

        if (to instanceof Refreshable) {
            decorator.canRefreshProperty().bind(((Refreshable) to).refreshableProperty());
        } else {
            decorator.canRefreshProperty().unbind();
            decorator.canRefreshProperty().set(false);
        }

        decorator.canCloseProperty().set(navigator.size() > 2);

        if (to instanceof DecoratorPage) {
            decorator.showCloseAsHomeProperty().set(!((DecoratorPage) to).isPageCloseable());
        } else {
            decorator.showCloseAsHomeProperty().set(true);
        }

        decorator.setNavigationDirection(event.getDirection());

        // state property should be updated at last.
        if (to instanceof DecoratorPage) {
            decorator.stateProperty().bind(((DecoratorPage) to).stateProperty());
        } else {
            decorator.stateProperty().unbind();
            decorator.stateProperty().set(new DecoratorPage.State("", null, navigator.canGoBack(), false, true));
        }

        if (to instanceof Region) {
            Region region = (Region) to;
            // Let root pane fix window size.
            StackPane parent = (StackPane) region.getParent();
            region.prefWidthProperty().bind(parent.widthProperty());
            region.prefHeightProperty().bind(parent.heightProperty());
        }
    }

    // ==== Dialog ====

    public void showDialog(Node node) {
        FXUtils.checkFxUserThread();

        if (dialog == null) {
            if (decorator.getDrawerWrapper() == null) {
                // Sometimes showDialog will be invoked before decorator was initialized.
                // Keep trying again.
                Platform.runLater(() -> showDialog(node));
                return;
            }
            dialog = new JFXDialog();
            dialogPane = new StackContainerPane();

            dialog.setContent(dialogPane);
            decorator.capableDraggingWindow(dialog);
            decorator.forbidDraggingWindow(dialogPane);
            dialog.setDialogContainer(decorator.getDrawerWrapper());
            dialog.setOverlayClose(false);
            dialog.show();

            navigator.setDisable(true);
        }
        dialogPane.push(node);

        EventHandler<DialogCloseEvent> handler = event -> closeDialog(node);
        node.getProperties().put(PROPERTY_DIALOG_CLOSE_HANDLER, handler);
        node.addEventHandler(DialogCloseEvent.CLOSE, handler);

        if (node instanceof DialogAware) {
            DialogAware dialogAware = (DialogAware) node;
            if (dialog.isVisible()) {
                dialogAware.onDialogShown();
            } else {
                dialog.visibleProperty().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        if (newValue) {
                            dialogAware.onDialogShown();
                            observable.removeListener(this);
                        }
                    }
                });
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void closeDialog(Node node) {
        FXUtils.checkFxUserThread();

        Optional.ofNullable(node.getProperties().get(PROPERTY_DIALOG_CLOSE_HANDLER))
                .ifPresent(handler -> node.removeEventHandler(DialogCloseEvent.CLOSE, (EventHandler<DialogCloseEvent>) handler));

        if (dialog != null) {
            dialogPane.pop(node);

            if (node instanceof DialogAware) {
                ((DialogAware) node).onDialogClosed();
            }

            if (dialogPane.getChildren().isEmpty()) {
                dialog.close();
                dialog = null;
                dialogPane = null;

                navigator.setDisable(false);
            }
        }
    }

    // ==== Toast ====

    public void showToast(String content) {
        decorator.getSnackbar().fireEvent(new JFXSnackbar.SnackbarEvent(content, null, 2000L, false, null));
    }

    // ==== Wizard ====

    public void startWizard(WizardProvider wizardProvider) {
        startWizard(wizardProvider, null);
    }

    public void startWizard(WizardProvider wizardProvider, String category) {
        FXUtils.checkFxUserThread();

        navigator.navigate(new DecoratorWizardDisplayer(wizardProvider, category), ContainerAnimations.FADE.getAnimationProducer());
    }

    // ==== Authlib Injector DnD ====

    private void setupAuthlibInjectorDnD() {
        decorator.addEventFilter(DragEvent.DRAG_OVER, AuthlibInjectorDnD.dragOverHandler());
        decorator.addEventFilter(DragEvent.DRAG_DROPPED, AuthlibInjectorDnD.dragDroppedHandler(
                url -> Controllers.dialog(new AddAuthlibInjectorServerPane(url))));
    }
}