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

import lombok.Getter;
import lombok.Setter;

public abstract class PaginationResult<T> {

    /**
     * The current page.
     */
    @Getter
    @Setter
    protected int currentPage;

    /**
     * The total results.
     */
    @Getter
    protected final int totalResults;

    /**
     * The per-page limit.
     */
    @Getter
    protected final int perPage;

    /**
     * Constructor.
     *
     * @param totalResults The total results
     * @param perPage The per-page limit
     */
    public PaginationResult(int totalResults, int perPage, int currentPage) {
        this.currentPage = currentPage;
        this.totalResults = totalResults;
        this.perPage = perPage;
    }

    /**
     * Get the current results.
     *
     * @return Results
     */
    public abstract Iterable<T> currentPageResults();

    /**
     * Check if these results have a next page.
     *
     * @return True if there's a next page
     */
    public boolean hasNextPage() {
        return currentPage < totalPages();
    }

    /**
     * Check if these results have a previous page.
     *
     * @return True if there's a previous page
     */
    public boolean hasPrevPage() {
        return currentPage > 1;
    }

    /**
     * Check if the results are empty.
     *
     * @return True if no results
     */
    public boolean isEmpty() {
        return totalResults == 0;
    }

    /**
     * Get the current limit.
     *
     * @return The limit
     */
    public int limit() {
        return offset() + perPage;
    }

    /**
     * Get the limit for a specific page.
     *
     * @param page The page
     * @return The limit
     */
    public int limitForPage(int page) {
        return offsetForPage(page) + perPage;
    }

    /**
     * Get the zero-based index offset.
     *
     * @return The offset
     */
    public int offset() {
        return offsetForPage(currentPage);
    }

    /**
     * Get the zero-based index offset for a specific page.
     *
     * @param page The page
     * @return The offset
     */
    public int offsetForPage(int page) {
        return perPage * (page - 1);
    }

    /**
     * Get the total number of pages.
     *
     * @return The total pages
     */
    public int totalPages() {
        return (int) Math.ceil(totalResults / (double) perPage);
    }
}
