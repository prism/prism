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

import java.util.function.Consumer;
import lombok.Getter;

@Getter
public class PaginationHandler<T> {

    @FunctionalInterface
    public interface Paginator {
        void paginate(int page);
    }

    /**
     * The pagination result.
     */
    private final PaginationResult<T> paginationResult;

    /**
     * The paginator.
     */
    private final Paginator paginator;

    /**
     * The subheader renderer.
     */
    private final Runnable subheader;

    /**
     * The line renderer.
     */
    private final Consumer<T> lineRenderer;

    /**
     * Constructor.
     *
     * @param paginationResult The pagination source
     * @param paginator The paginator
     * @param lineRenderer The line renderer
     */
    public PaginationHandler(PaginationResult<T> paginationResult, Paginator paginator, Consumer<T> lineRenderer) {
        this(paginationResult, paginator, () -> {}, lineRenderer);
    }

    /**
     * Constructor.
     *
     * @param paginationResult The pagination source
     * @param paginator The paginator
     * @param subheader The subheader renderer
     * @param lineRenderer The line renderer
     */
    public PaginationHandler(
        PaginationResult<T> paginationResult,
        Paginator paginator,
        Runnable subheader,
        Consumer<T> lineRenderer
    ) {
        this.paginationResult = paginationResult;
        this.paginator = paginator;
        this.subheader = subheader;
        this.lineRenderer = lineRenderer;
    }

    /**
     * Show a specific page.
     *
     * @param page The page
     */
    public void showPage(int page) {
        this.paginator.paginate(page);
    }
}
