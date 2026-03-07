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

package org.prism_mc.prism.core.storage.adapters.mysql;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.jooq.DSLContext;
import org.jooq.JoinType;
import org.prism_mc.prism.core.storage.adapters.sql.SqlActivityQueryBuilder;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;

public class MysqlActivityQueryBuilder extends SqlActivityQueryBuilder {

    /**
     * Construct a new query builder.
     *
     * @param configurationService The configuration service
     * @param create The DSL context
     */
    @Inject
    public MysqlActivityQueryBuilder(ConfigurationService configurationService, @Assisted DSLContext create) {
        super(configurationService, create);
    }

    @Override
    protected JoinType actionJoinType() {
        /* This has to be a left join because when it's an inner join, MySQL/MariaDB
           treat it as a driving table which causes them to choose the actionId index
           over the drastically better composite index. */
        return JoinType.LEFT_OUTER_JOIN;
    }
}
