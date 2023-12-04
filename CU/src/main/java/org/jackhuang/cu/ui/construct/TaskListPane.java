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
package org.jackhuang.CU.ui.construct;

import com.jfoenix.controls.JFXProgressBar;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.jackhuang.CU.download.fabric.FabricAPIInstallTask;
import org.jackhuang.CU.download.fabric.FabricInstallTask;
import org.jackhuang.CU.download.forge.ForgeNewInstallTask;
import org.jackhuang.CU.download.forge.ForgeOldInstallTask;
import org.jackhuang.CU.download.game.GameAssetDownloadTask;
import org.jackhuang.CU.download.game.GameInstallTask;
import org.jackhuang.CU.download.java.JavaDownloadTask;
import org.jackhuang.CU.download.liteloader.LiteLoaderInstallTask;
import org.jackhuang.CU.download.optifine.OptiFineInstallTask;
import org.jackhuang.CU.download.quilt.QuiltAPIInstallTask;
import org.jackhuang.CU.download.quilt.QuiltInstallTask;
import org.jackhuang.CU.game.CUModpackInstallTask;
import org.jackhuang.CU.mod.MinecraftInstanceTask;
import org.jackhuang.CU.mod.ModpackInstallTask;
import org.jackhuang.CU.mod.ModpackUpdateTask;
import org.jackhuang.CU.mod.curse.CurseCompletionTask;
import org.jackhuang.CU.mod.curse.CurseInstallTask;
import org.jackhuang.CU.mod.mcbbs.McbbsModpackCompletionTask;
import org.jackhuang.CU.mod.mcbbs.McbbsModpackExportTask;
import org.jackhuang.CU.mod.modrinth.ModrinthCompletionTask;
import org.jackhuang.CU.mod.modrinth.ModrinthInstallTask;
import org.jackhuang.CU.mod.multimc.MultiMCModpackExportTask;
import org.jackhuang.CU.mod.multimc.MultiMCModpackInstallTask;
import org.jackhuang.CU.mod.server.ServerModpackCompletionTask;
import org.jackhuang.CU.mod.server.ServerModpackExportTask;
import org.jackhuang.CU.mod.server.ServerModpackLocalInstallTask;
import org.jackhuang.CU.setting.Theme;
import org.jackhuang.CU.task.Task;
import org.jackhuang.CU.task.TaskExecutor;
import org.jackhuang.CU.task.TaskListener;
import org.jackhuang.CU.ui.FXUtils;
import org.jackhuang.CU.ui.SVG;
import org.jackhuang.CU.util.Lang;
import org.jackhuang.CU.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jackhuang.CU.ui.FXUtils.runInFX;
import static org.jackhuang.CU.util.Lang.tryCast;
import static org.jackhuang.CU.util.i18n.I18n.i18n;

public final class TaskListPane extends StackPane {
    private TaskExecutor executor;
    private final AdvancedListBox listBox = new AdvancedListBox();
    private final Map<Task<?>, ProgressListNode> nodes = new HashMap<>();
    private final List<StageNode> stageNodes = new ArrayList<>();
    private final ObjectProperty<Insets> progressNodePadding = new SimpleObjectProperty<>(Insets.EMPTY);

    public TaskListPane() {
        listBox.setSpacing(0);

        getChildren().setAll(listBox);
    }

    public void setExecutor(TaskExecutor executor) {
        List<String> stages = Lang.removingDuplicates(executor.getStages());
        this.executor = executor;
        executor.addTaskListener(new TaskListener() {
            @Override
            public void onStart() {
                Platform.runLater(() -> {
                    stageNodes.clear();
                    listBox.clear();
                    stageNodes.addAll(stages.stream().map(StageNode::new).collect(Collectors.toList()));
                    stageNodes.forEach(listBox::add);

                    if (stages.isEmpty()) progressNodePadding.setValue(new Insets(0, 0, 8, 0));
                    else progressNodePadding.setValue(new Insets(0, 0, 8, 26));
                });
            }

            @Override
            public void onReady(Task<?> task) {
                if (task.getStage() != null) {
                    Platform.runLater(() -> {
                        stageNodes.stream().filter(x -> x.stage.equals(task.getStage())).findAny().ifPresent(StageNode::begin);
                    });
                }
            }

            @Override
            public void onRunning(Task<?> task) {
                if (!task.getSignificance().shouldShow() || task.getName() == null)
                    return;

                if (task instanceof GameAssetDownloadTask) {
                    task.setName(i18n("assets.download_all"));
                } else if (task instanceof GameInstallTask) {
                    task.setName(i18n("install.installer.install", i18n("install.installer.game")));
                } else if (task instanceof ForgeNewInstallTask || task instanceof ForgeOldInstallTask) {
                    task.setName(i18n("install.installer.install", i18n("install.installer.forge")));
                } else if (task instanceof LiteLoaderInstallTask) {
                    task.setName(i18n("install.installer.install", i18n("install.installer.liteloader")));
                } else if (task instanceof OptiFineInstallTask) {
                    task.setName(i18n("install.installer.install", i18n("install.installer.optifine")));
                } else if (task instanceof FabricInstallTask) {
                    task.setName(i18n("install.installer.install", i18n("install.installer.fabric")));
                } else if (task instanceof FabricAPIInstallTask) {
                    task.setName(i18n("install.installer.install", i18n("install.installer.fabric-api")));
                } else if (task instanceof QuiltInstallTask) {
                    task.setName(i18n("install.installer.install", i18n("install.installer.quilt")));
                } else if (task instanceof QuiltAPIInstallTask) {
                    task.setName(i18n("install.installer.install", i18n("install.installer.quilt-api")));
                } else if (task instanceof CurseCompletionTask || task instanceof ModrinthCompletionTask || task instanceof ServerModpackCompletionTask || task instanceof McbbsModpackCompletionTask) {
                    task.setName(i18n("modpack.completion"));
                } else if (task instanceof ModpackInstallTask) {
                    task.setName(i18n("modpack.installing"));
                } else if (task instanceof ModpackUpdateTask) {
                    task.setName(i18n("modpack.update"));
                } else if (task instanceof CurseInstallTask) {
                    task.setName(i18n("modpack.install", i18n("modpack.type.curse")));
                } else if (task instanceof MultiMCModpackInstallTask) {
                    task.setName(i18n("modpack.install", i18n("modpack.type.multimc")));
                } else if (task instanceof ModrinthInstallTask) {
                    task.setName(i18n("modpack.install", i18n("modpack.type.modrinth")));
                } else if (task instanceof ServerModpackLocalInstallTask) {
                    task.setName(i18n("modpack.install", i18n("modpack.type.server")));
                } else if (task instanceof CUModpackInstallTask) {
                    task.setName(i18n("modpack.install", i18n("modpack.type.CU")));
                } else if (task instanceof McbbsModpackExportTask || task instanceof MultiMCModpackExportTask || task instanceof ServerModpackExportTask) {
                    task.setName(i18n("modpack.export"));
                } else if (task instanceof MinecraftInstanceTask) {
                    task.setName(i18n("modpack.scan"));
                } else if (task instanceof JavaDownloadTask) {
                    task.setName(i18n("download.java"));
                }

                Platform.runLater(() -> {
                    ProgressListNode node = new ProgressListNode(task);
                    nodes.put(task, node);
                    StageNode stageNode = stageNodes.stream().filter(x -> x.stage.equals(task.getInheritedStage())).findAny().orElse(null);
                    listBox.add(listBox.indexOf(stageNode) + 1, node);
                });
            }

            @Override
            public void onFinished(Task<?> task) {
                if (task.getStage() != null) {
                    Platform.runLater(() -> {
                        stageNodes.stream().filter(x -> x.stage.equals(task.getStage())).findAny().ifPresent(StageNode::succeed);
                    });
                }

                Platform.runLater(() -> {
                    ProgressListNode node = nodes.remove(task);
                    if (node == null)
                        return;
                    node.unbind();
                    listBox.remove(node);
                });
            }

            @Override
            public void onFailed(Task<?> task, Throwable throwable) {
                if (task.getStage() != null) {
                    Platform.runLater(() -> {
                        stageNodes.stream().filter(x -> x.stage.equals(task.getStage())).findAny().ifPresent(StageNode::fail);
                    });
                }
                ProgressListNode node = nodes.remove(task);
                if (node == null)
                    return;
                Platform.runLater(() -> {
                    node.setThrowable(throwable);
                });
            }

            @Override
            public void onPropertiesUpdate(Task<?> task) {
                if (task instanceof Task.CountTask) {
                    runInFX(() -> {
                        stageNodes.stream()
                                .filter(x -> x.stage.equals(((Task<?>.CountTask) task).getCountStage()))
                                .findAny()
                                .ifPresent(StageNode::count);
                    });

                    return;
                }

                if (task.getStage() != null) {
                    int total = tryCast(task.getProperties().get("total"), Integer.class).orElse(0);
                    runInFX(() -> {
                        stageNodes.stream()
                                .filter(x -> x.stage.equals(task.getStage()))
                                .findAny()
                                .ifPresent(stageNode -> {
                                    stageNode.setTotal(total);
                                });
                    });
                }
            }
        });
    }

    private static class StageNode extends BorderPane {
        private final String stage;
        private final Label title = new Label();
        private final String message;
        private int count = 0;
        private int total = 0;
        private boolean started = false;

        public StageNode(String stage) {
            this.stage = stage;

            String stageKey = StringUtils.substringBefore(stage, ':');
            String stageValue = StringUtils.substringAfter(stage, ':');

            // @formatter:off
            switch (stageKey) {
                case "CU.modpack": message = i18n("install.modpack"); break;
                case "CU.modpack.download": message = i18n("launch.state.modpack"); break;
                case "CU.install.assets": message = i18n("assets.download"); break;
                case "CU.install.game": message = i18n("install.installer.install", i18n("install.installer.game") + " " + stageValue); break;
                case "CU.install.forge": message = i18n("install.installer.install", i18n("install.installer.forge") + " " + stageValue); break;
                case "CU.install.liteloader": message = i18n("install.installer.install", i18n("install.installer.liteloader") + " " + stageValue); break;
                case "CU.install.optifine": message = i18n("install.installer.install", i18n("install.installer.optifine") + " " + stageValue); break;
                case "CU.install.fabric": message = i18n("install.installer.install", i18n("install.installer.fabric") + " " + stageValue); break;
                case "CU.install.fabric-api": message = i18n("install.installer.install", i18n("install.installer.fabric-api") + " " + stageValue); break;
                case "CU.install.quilt": message = i18n("install.installer.install", i18n("install.installer.quilt") + " " + stageValue); break;
                default: message = i18n(stageKey); break;
            }
            // @formatter:on

            title.setText(message);
            BorderPane.setAlignment(title, Pos.CENTER_LEFT);
            BorderPane.setMargin(title, new Insets(0, 0, 0, 8));
            setPadding(new Insets(0, 0, 8, 4));
            setCenter(title);
            setLeft(FXUtils.limitingSize(SVG.dotsHorizontal(Theme.blackFillBinding(), 14, 14), 14, 14));
        }

        public void begin() {
            if (started) return;
            started = true;
            setLeft(FXUtils.limitingSize(SVG.arrowRight(Theme.blackFillBinding(), 14, 14), 14, 14));
        }

        public void fail() {
            setLeft(FXUtils.limitingSize(SVG.close(Theme.blackFillBinding(), 14, 14), 14, 14));
        }

        public void succeed() {
            setLeft(FXUtils.limitingSize(SVG.check(Theme.blackFillBinding(), 14, 14), 14, 14));
        }

        public void count() {
            updateCounter(++count, total);
        }

        public void setTotal(int total) {
            this.total = total;
            updateCounter(count, total);
        }

        public void updateCounter(int count, int total) {
            if (total > 0)
                title.setText(String.format("%s - %d/%d", message, count, total));
            else
                title.setText(message);
        }
    }

    private class ProgressListNode extends BorderPane {
        private final JFXProgressBar bar = new JFXProgressBar();
        private final Label title = new Label();
        private final Label state = new Label();
        private final DoubleBinding binding = Bindings.createDoubleBinding(() ->
                        getWidth() - getPadding().getLeft() - getPadding().getRight(),
                paddingProperty(), widthProperty());

        public ProgressListNode(Task<?> task) {
            bar.progressProperty().bind(task.progressProperty());
            title.setText(task.getName());
            state.textProperty().bind(task.messageProperty());

            setLeft(title);
            setRight(state);
            setBottom(bar);

            bar.minWidthProperty().bind(binding);
            bar.prefWidthProperty().bind(binding);
            bar.maxWidthProperty().bind(binding);

            paddingProperty().bind(progressNodePadding);
        }

        public void unbind() {
            bar.progressProperty().unbind();
            state.textProperty().unbind();
        }

        public void setThrowable(Throwable throwable) {
            unbind();
            state.setText(throwable.getLocalizedMessage());
            bar.setProgress(0);
        }
    }
}
