/*
 * This file is a part of prism-idb.
 *
 * MIT License
 *
 * Copyright (c) 2014-2018 Daniel Ennis
 * Copyright 2022 viveleroi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package network.darkhelmet.prism.idb;

import com.zaxxer.hikari.HikariConfig;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Builder(toBuilder = true)
@Data
public class PooledDatabaseOptions {
    @Builder.Default int minIdleConnections = 3;
    @Builder.Default int maxConnections = 5;
    Map<String, Object> dataSourceProperties;
    DatabaseOptions options;
    HikariConfig hikariConfig;

    public static class PooledDatabaseOptionsBuilder  {
        /**
         * Create a new hikari database.
         *
         * @return The hikari database
         */
        public HikariPooledDatabase createHikariDatabase() {
            if (hikariConfig != null) {
                String url = hikariConfig.getDataSourceProperties().getProperty("url");
                String dsn = url.replace("jdbc:", "");

                DatabaseOptions.DatabaseOptionsBuilder builder = DatabaseOptions.builder().dsn(dsn);
                options = builder.build();

                return new HikariPooledDatabase(this.build(), hikariConfig);
            }

            return new HikariPooledDatabase(this.build());
        }
    }
}
