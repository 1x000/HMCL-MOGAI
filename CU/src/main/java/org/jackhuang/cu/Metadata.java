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
package org.jackhuang.CU;

import org.jackhuang.CU.util.StringUtils;
import org.jackhuang.CU.util.io.JarUtils;
import org.jackhuang.CU.util.platform.OperatingSystem;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Stores metadata about this application.
 */
public final class Metadata {
    private Metadata() {}

    public static final String NAME = "CU";
    public static final String FULL_NAME = "Hello Minecraft! Launcher";
    public static final String VERSION = System.getProperty("CU.version.override", JarUtils.getManifestAttribute("Implementation-Version", "@develop@"));

    public static final String TITLE = NAME + " " + VERSION;
    public static final String FULL_TITLE = FULL_NAME + " v" + VERSION;

    public static final String UPDATE_URL = System.getProperty("CU.update_source.override", "https://CU.huangyuhui.net/api/update_link");
    public static final String CONTACT_URL = "https://docs.CU.net/help.html";
    public static final String HELP_URL = "https://CU.huangyuhui.net/help";
    public static final String CHANGELOG_URL = "https://docs.CU.net/changelog/";
    public static final String PUBLISH_URL = "https://www.mcbbs.net/thread-142335-1-1.html";
    public static final String EULA_URL = "https://docs.CU.net/eula/CU.html";

    public static final String BUILD_CHANNEL = JarUtils.getManifestAttribute("Build-Channel", "nightly");
    public static final String GITHUB_SHA = JarUtils.getManifestAttribute("GitHub-SHA", null);

    public static final Path MINECRAFT_DIRECTORY = OperatingSystem.getWorkingDirectory("minecraft");
    public static final Path CU_DIRECTORY;

    static {
        String CUHome = System.getProperty("CU.home");
        if (CUHome == null) {
            if (OperatingSystem.CURRENT_OS == OperatingSystem.LINUX) {
                String xdgData = System.getenv("XDG_DATA_HOME");
                if (StringUtils.isNotBlank(xdgData)) {
                    CU_DIRECTORY = Paths.get(xdgData, "CU").toAbsolutePath();
                } else {
                    CU_DIRECTORY = Paths.get(System.getProperty("user.home", "."), ".local", "share", "CU").toAbsolutePath();
                }
            } else {
                CU_DIRECTORY = OperatingSystem.getWorkingDirectory("CU");
            }
        } else {
            CU_DIRECTORY = Paths.get(CUHome).toAbsolutePath().normalize();
        }
    }

    public static boolean isStable() {
        return "stable".equals(BUILD_CHANNEL);
    }

    public static boolean isDev() {
        return "dev".equals(BUILD_CHANNEL);
    }

    public static boolean isNightly() {
        return !isStable() && !isDev();
    }
}
