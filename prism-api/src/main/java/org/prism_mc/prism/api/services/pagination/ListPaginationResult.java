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

package org.prism_mc.prism.api.services.pagination;

import java.util.List;
import lombok.Getter;

public class ListPaginationResult<T> extends PaginationResult<T> {

    /**
     * The results.
     */
    @Getter
    protected final List<T> results;

    /**
     * Constructor.
     *
     * @param results The results
     * @param perPage The per-page limit
     */
    public ListPaginationResult(List<T> results, int perPage) {
        this(results, results.size(), perPage, 1);
    }

    /**
     * Constructor.
     *
     * @param results The results
     * @param perPage The per-page limit
     */
    public ListPaginationResult(List<T> results, int totalResults, int perPage, int currentPage) {
        super(totalResults, perPage, currentPage);
        this.results = results;
    }

    @Override
    public Iterable<T> currentPageResults() {
        int startIndex = Math.max(0, offset());
        int endIndex = Math.min(results.size(), startIndex + perPage);

        if (startIndex >= results.size()) {
            return List.of();
        }

        return results.subList(startIndex, endIndex);
    }
}
