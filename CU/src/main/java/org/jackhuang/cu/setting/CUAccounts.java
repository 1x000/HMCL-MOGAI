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
package org.jackhuang.CU.setting;

import com.google.gson.annotations.SerializedName;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jackhuang.CU.auth.OAuth;
import org.jackhuang.CU.task.Schedulers;
import org.jackhuang.CU.task.Task;
import org.jackhuang.CU.util.gson.JsonUtils;
import org.jackhuang.CU.util.gson.UUIDTypeAdapter;
import org.jackhuang.CU.util.io.HttpRequest;
import org.jackhuang.CU.util.io.NetworkUtils;

import java.util.UUID;

import static org.jackhuang.CU.util.Lang.mapOf;
import static org.jackhuang.CU.util.Pair.pair;

public final class CUAccounts {

    private static final ObjectProperty<CUAccount> account = new SimpleObjectProperty<>();

    private CUAccounts() {
    }

    public static CUAccount getAccount() {
        return account.get();
    }

    public static ObjectProperty<CUAccount> accountProperty() {
        return account;
    }

    public static void setAccount(CUAccount account) {
        CUAccounts.account.set(account);
    }

    public static Task<Void> login() {
        String nonce = UUIDTypeAdapter.fromUUID(UUID.randomUUID());
        String scope = "openid offline_access";

        return Task.supplyAsync(() -> {
            OAuth.Session session = Accounts.OAUTH_CALLBACK.startServer();
            Accounts.OAUTH_CALLBACK.openBrowser(NetworkUtils.withQuery(
                    "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
                    mapOf(
                            pair("client_id", Accounts.OAUTH_CALLBACK.getClientId()),
                            pair("response_type", "id_token code"),
                            pair("response_mode", "form_post"),
                            pair("scope", scope),
                            pair("redirect_uri", session.getRedirectURI()),
                            pair("nonce", nonce)
                    )));
            String code = session.waitFor();

            // Authorization Code -> Token
            String responseText = HttpRequest.POST("https://login.microsoftonline.com/common/oauth2/v2.0/token")
                    .form(mapOf(pair("client_id", Accounts.OAUTH_CALLBACK.getClientId()), pair("code", code),
                            pair("grant_type", "authorization_code"), pair("client_secret", Accounts.OAUTH_CALLBACK.getClientSecret()),
                            pair("redirect_uri", session.getRedirectURI()), pair("scope", scope)))
                    .getString();
            OAuth.AuthorizationResponse response = JsonUtils.fromNonNullJson(responseText,
                    OAuth.AuthorizationResponse.class);

            CUAccountProfile profile = HttpRequest.GET("https://CU.huangyuhui.net/api/user")
                    .header("Token-Type", response.tokenType)
                    .header("Access-Token", response.accessToken)
                    .header("Authorization-Provider", "microsoft")
                    .authorization("Bearer", session.getIdToken())
                    .getJson(CUAccountProfile.class);

            return new CUAccount("microsoft", profile.nickname, profile.email, profile.role, session.getIdToken(), response.tokenType, response.accessToken, response.refreshToken);
        }).thenAcceptAsync(Schedulers.javafx(), account -> {
            setAccount(account);
        });
    }

    public static class CUAccount implements HttpRequest.Authorization {
        private final String provider;
        private final String nickname;
        private final String email;
        private final String role;
        private final String idToken;
        private final String tokenType;
        private final String accessToken;
        private final String refreshToken;

        public CUAccount(String provider, String nickname, String email, String role, String idToken, String tokenType, String accessToken, String refreshToken) {
            this.provider = provider;
            this.nickname = nickname;
            this.email = email;
            this.role = role;
            this.idToken = idToken;
            this.tokenType = tokenType;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getProvider() {
            return provider;
        }

        public String getNickname() {
            return nickname;
        }

        public String getEmail() {
            return email;
        }

        public String getRole() {
            return role;
        }

        public String getIdToken() {
            return idToken;
        }

        @Override
        public String getTokenType() {
            return tokenType;
        }

        @Override
        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }

    private static class CUAccountProfile {
        @SerializedName("ID")
        String id;
        @SerializedName("Provider")
        String provider;
        @SerializedName("Email")
        String email;
        @SerializedName("NickName")
        String nickname;
        @SerializedName("Role")
        String role;
    }

}
