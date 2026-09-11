/*
 * prism
 *
 * Copyright (c) 2022 M Botsko (viveleroi)
 *                    Contributors
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

package org.prism_mc.prism.loader.services.configuration;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
@Getter
public class WebConfiguration {

    @Comment(
        """
        Master switch for the embedded web server feature. When false, the web server cannot run
        and the "prism web start" command is refused."""
    )
    private boolean enabled = false;

    @Comment(
        """
        Automatically start the embedded web server when the plugin loads. Requires enabled to be
        true. The "prism web start" and "prism web stop" commands can control it at runtime."""
    )
    private boolean autoStart = false;

    @Comment("The port the web server listens on.")
    private int port = 4040;

    @Comment(
        """
        The network interface the web server binds to. Use "0.0.0.0" to listen on all interfaces,
        or "127.0.0.1" to restrict access to the local machine (e.g. when behind a reverse proxy)."""
    )
    private String bindAddress = "0.0.0.0";

    @Comment("API key for authenticating web requests. Set this to a secure random value before enabling.")
    private String apiKey = "";

    @Comment(
        """
        Browser origins permitted to call the web API cross-origin (CORS).
        Only needed when the web UI is hosted separately from this server."""
    )
    private List<String> allowedOrigins = new ArrayList<>();

    @Comment("Maximum number of results returned per query.")
    private int maxResults = 1000;

    @Comment(
        """
        The default time range applied to activity queries when the user has not supplied a "since"
        filter. Accepts a relative time string (e.g. "3d", "12h", "1w"). Constraining the default
        query to a recent window lets it scan far fewer rows. Leave empty to apply no default range
        (not recommended on large databases)."""
    )
    private String defaultActivityRange = "3d";

    @Comment(
        """
        The base path the web UI and API are served under. Leave empty to serve from the domain
        root. Set this (e.g. "/prism") to host behind a reverse proxy on a sub-path. When set, the
        proxy must forward the full path WITHOUT stripping the prefix (in Caddy use "handle", not
        "handle_path"; in nginx do not rewrite the path)."""
    )
    private String basePath = "";

    /**
     * The normalized context prefix derived from {@link #basePath}: empty, or a leading-slash path
     * with no trailing slash (e.g. "/prism"). Used to register HTTP contexts and to strip the
     * prefix from incoming requests.
     *
     * @return The context prefix
     */
    public String contextPrefix() {
        String trimmed = basePath.replaceAll("^/+", "").replaceAll("/+$", "");
        return trimmed.isEmpty() ? "" : "/" + trimmed;
    }

    /**
     * The base href derived from {@link #basePath}, always slash-wrapped (e.g. "/" or "/prism/").
     * Injected into served HTML/JS/CSS so the client requests assets and the API under the prefix.
     *
     * @return The base href
     */
    public String baseHref() {
        String prefix = contextPrefix();
        return prefix.isEmpty() ? "/" : prefix + "/";
    }
}
