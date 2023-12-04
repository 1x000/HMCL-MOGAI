# CU启动器

## 简介

稍微对CU进行了一点改造

## 开源协议

GPL-3.0

## 贡献
如果您想提交一个 Pull Request, 必须遵守如下要求:
* IDE: Intellij IDEA
* 编译器: Java 1.8
* **不要**修改 `gradle` 相关文件

### 编译
于项目根目录执行以下命令:

```bash
./gradlew clean build
```

请确保您至少安装了含有 JavaFX 8 的 Java. 建议使用 Liberica Full JDK 8 或更高版本.

## JVM 选项 (用于调试)
| 参数                                           | 简介                                                                                              |
|----------------------------------------------|-------------------------------------------------------------------------------------------------|
| `-DCU.home=<path>`                         | 覆盖 CU 数据文件夹.                                                                                  |
| `-DCU.self_integrity_check.disable=true`   | 检查更新时绕过本体完整性检查.                                                                                 |
| `-DCU.bmclapi.override=<version>`          | 覆盖 BMCLAPI 的 API Root, 默认值为 `https://bmclapi2.bangbang93.com`. 例如 `https://download.mcbbs.net`. |
| `-DCU.font.override=<font family>`         | 覆盖字族.                                                                                           |
| `-DCU.version.override=<version>`          | 覆盖版本号.                                                                                          |
| `-DCU.update_source.override=<url>`        | 覆盖更新源.                                                                                          |
| `-DCU.authlibinjector.location=<path>`     | 使用指定的 authlib-injector (而非下载一个).                                                                |
| `-DCU.openjfx.repo=<maven repository url>` | 添加用于下载 OpenJFX 的自定义 Maven 仓库                                                                    |
| `-DCU.native.encoding=<encoding>`          | 覆盖原生编码.                                                                                         |
| `-DCU.microsoft.auth.id=<App ID>`          | 覆盖 Microsoft OAuth App ID.                                                                      |
| `-DCU.microsoft.auth.secret=<App Secret>`  | 覆盖 Microsoft OAuth App 密钥.                                                                      |
