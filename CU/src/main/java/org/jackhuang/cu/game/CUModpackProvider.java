/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2022  huangyuhui <huanghongxun2008@126.com> and contributors
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
package org.jackhuang.CU.game;

import com.google.gson.JsonParseException;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jackhuang.CU.download.DefaultDependencyManager;
import org.jackhuang.CU.mod.MismatchedModpackTypeException;
import org.jackhuang.CU.mod.Modpack;
import org.jackhuang.CU.mod.ModpackProvider;
import org.jackhuang.CU.mod.ModpackUpdateTask;
import org.jackhuang.CU.setting.Profile;
import org.jackhuang.CU.task.Task;
import org.jackhuang.CU.util.StringUtils;
import org.jackhuang.CU.util.gson.JsonUtils;
import org.jackhuang.CU.util.io.CompressingUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

public final class CUModpackProvider implements ModpackProvider {
    public static final CUModpackProvider INSTANCE = new CUModpackProvider();

    @Override
    public String getName() {
        return "CU";
    }

    @Override
    public Task<?> createCompletionTask(DefaultDependencyManager dependencyManager, String version) {
        return null;
    }

    @Override
    public Task<?> createUpdateTask(DefaultDependencyManager dependencyManager, String name, File zipFile, Modpack modpack) throws MismatchedModpackTypeException {
        if (!(modpack.getManifest() instanceof CUModpackManifest))
            throw new MismatchedModpackTypeException(getName(), modpack.getManifest().getProvider().getName());

        if (!(dependencyManager.getGameRepository() instanceof CUGameRepository)) {
            throw new IllegalArgumentException("CUModpackProvider requires CUGameRepository");
        }

        CUGameRepository repository = (CUGameRepository) dependencyManager.getGameRepository();
        Profile profile = repository.getProfile();

        return new ModpackUpdateTask(dependencyManager.getGameRepository(), name, new CUModpackInstallTask(profile, zipFile, modpack, name));
    }

    @Override
    public Modpack readManifest(ZipFile file, Path path, Charset encoding) throws IOException, JsonParseException {
        String manifestJson = CompressingUtils.readTextZipEntry(file, "modpack.json");
        Modpack manifest = JsonUtils.fromNonNullJson(manifestJson, CUModpack.class).setEncoding(encoding);
        String gameJson = CompressingUtils.readTextZipEntry(file, "minecraft/pack.json");
        Version game = JsonUtils.fromNonNullJson(gameJson, Version.class);
        if (game.getJar() == null)
            if (StringUtils.isBlank(manifest.getVersion()))
                throw new JsonParseException("Cannot recognize the game version of modpack " + file + ".");
            else
                manifest.setManifest(CUModpackManifest.INSTANCE);
        else
            manifest.setManifest(CUModpackManifest.INSTANCE).setGameVersion(game.getJar());
        return manifest;
    }

    private static class CUModpack extends Modpack {
        @Override
        public Task<?> getInstallTask(DefaultDependencyManager dependencyManager, File zipFile, String name) {
            return new CUModpackInstallTask(((CUGameRepository) dependencyManager.getGameRepository()).getProfile(), zipFile, this, name);
        }
    }

}
