/*
 * This file is part of LuckPerms, licensed under the MIT License.
 * It has been modified for use in Prism.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package org.prism_mc.prism.loader.services.dependencies;

import com.google.common.collect.ImmutableList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.prism_mc.prism.loader.services.dependencies.relocation.Relocation;

public enum Dependency {
    ASM("org.ow2.asm", "asm", "9.8", "h26raoPa7K1cpn65/KuwY8l7WuuM8fynqYns3hdSIFE="),
    ASM_COMMONS("org.ow2.asm", "asm-commons", "9.8", "MwGhwctMWfzFKSZI2sHXxa7UwPBn376IhzuM3+d0BPQ="),
    JAR_RELOCATOR("me.lucko", "jar-relocator", "1.7", "b30RhOF6kHiHl+O5suNLh/+eAr1iOFEFLXhwkHHDu4I="),

    AOPALLIANCE(
        "aopalliance",
        "aopalliance",
        "1.0",
        "Ct3sZw/tzT8RPFyAkdeDKA0j9146y4QbYanNsHk3agg=",
        Relocation.of("aopalliance", "org{}aopalliance")
    ),
    CAFFEINE(
        "com.github.ben-manes.caffeine",
        "caffeine",
        "3.2.0",
        "7EEd/fDAPyUhhkjOiYYWMLcWgOWFippyeOusjlXKs9c=",
        Relocation.of("caffeine", "com{}github{}benmanes{}caffeine")
    ),
    CONFIGURATE_CORE(
        "org.spongepowered",
        "configurate-core",
        "4.2.0",
        "BsHp93iaGJrwwBVuvp9GnafMZ0Iz9D6BM8gxMe3Z9+A=",
        Relocation.of("configurate", "org{}spongepowered{}configurate"),
        Relocation.of("geantyref", "io{}leangen{}geantyref")
    ),
    CONFIGURATE_HOCON(
        "org.spongepowered",
        "configurate-hocon",
        "4.2.0",
        "/xN1mkZZmBB/iJgVB1G50M93tbX/ubXsFszqnPIpiT4=",
        Relocation.of("configurate", "org{}spongepowered{}configurate"),
        Relocation.of("hocon", "com{}typesafe{}config")
    ),
    CRON_UTILS(
        "com.cronutils",
        "cron-utils",
        "9.2.1",
        "Aq8Oiy/pPJ+m7s+XtTs5+q4UxbmWNW7bEy6f5iABN0Q=",
        Relocation.of("cronutils", "com{}cronutils")
    ),
    GEANTYREF(
        "io{}leangen{}geantyref",
        "geantyref",
        "1.3.13",
        "1t8GCrD79sWLDcgUViKrd0vzv5WL0/62d0zI0851bto=",
        Relocation.of("geantyref", "io{}leangen{}geantyref")
    ),
    GUICE(
        "com{}google{}inject",
        "guice",
        "7.0.0",
        "3lsONZvXsDykKAazaIRu/ZVIQ4D+Ba4qTqcbwzjFnAA=",
        Relocation.of("inject", "com{}google{}inject"),
        Relocation.of("aopalliance", "org{}aopalliance"),
        Relocation.of("jakartainject", "jakarta{}inject")
    ),
    GUICE_ASSISTED(
        "com{}google{}inject{}extensions",
        "guice-assistedinject",
        "7.0.0",
        "wHEl//k3K9ZWLphryfMJJpTblkmnl2FoOtydpv/rM+Y=",
        Relocation.of("inject", "com{}google{}inject"),
        Relocation.of("jakartainject", "jakarta{}inject")
    ),
    H2_DRIVER(
        "com.h2database",
        "h2",
        "2.3.232",
        "ja5i0i24mCw9yzgm7bnHJ8XTAgY6Z+731j2C3kAfB9M=",
        Relocation.of("h2", "com{}h2database")
    ),
    HIKARI(
        "com.zaxxer",
        "HikariCP",
        "6.3.0",
        "B8Y0QFmvMKE1FEIJx8i9ZmuIIxJEIuyFmGTSCdSrfKE=",
        Relocation.of("hikari", "com{}zaxxer{}hikari")
    ),
    HOCON_CONFIG(
        "com{}typesafe",
        "config",
        "1.4.2",
        "AHbCSbQ4fYNpFGUo/V2ss++6CY3ALs+ayB3r38LhL9U=",
        Relocation.of("hocon", "com{}typesafe{}config")
    ),
    JACKSON_ANNOTATIONS(
        "com{}fasterxml{}jackson{}core",
        "jackson-annotations",
        "2.18.3",
        "iqV0DYC1pQJVCLQbutuqH7N3ImfGKLLjBoGk9F+LiTE=",
        Relocation.of("jackson", "com{}fasterxml{}jackson")
    ),
    JACKSON_CORE(
        "com{}fasterxml{}jackson{}core",
        "jackson-core",
        "2.18.3",
        "BWvE0+XlPOghRQ+pez+eD43eElz22miENTux8JWC4dk=",
        Relocation.of("jackson", "com{}fasterxml{}jackson")
    ),
    JACKSON_DATABIND(
        "com{}fasterxml{}jackson{}core",
        "jackson-databind",
        "2.18.3",
        "UQvdp1p6YYbFvzO4USOUiKFFCQauV1cSHy4cxIp+EI8=",
        Relocation.of("jackson", "com{}fasterxml{}jackson")
    ),
    JAKARTA_INJECT(
        "jakarta{}inject",
        "jakarta.inject-api",
        "2.0.1",
        "99yYBi/M8UEmq7dRtk+rEsMSVm6MvchINZi//OqTr3w=",
        Relocation.of("jakartainject", "jakarta{}inject")
    ),
    JOOQ(
        "org.jooq",
        "jooq",
        "3.17.35",
        "7IjJxH0MM06u28/z9wQbckRBdhHTniIvwYT5pkvWsq0=",
        Relocation.of("jooq", "org{}jooq"),
        Relocation.of("reactivestreams", "org{}reactivestreams"),
        Relocation.of("r2dbc", "org{}r2dbc")
    ),
    MARIADB_DRIVER(
        "org.mariadb.jdbc",
        "mariadb-java-client",
        "3.5.3",
        "hcS6LyIdDf1DnCav+7KU94SWB2NUQmPGWrqcLHaFhwY=",
        Relocation.of("mariadb", "org{}mariadb")
    ),
    MOONSHINE_CORE(
        "net{}kyori{}moonshine",
        "moonshine-core",
        "2.0.4",
        "YB1EB6/u6yXplav0Gm7r1hEL5EE6LLCi4XKqmhlh3Fc=",
        Relocation.of("kyori.moonshine", "net{}kyori{}moonshine"),
        Relocation.of("geantyref", "io{}leangen{}geantyref")
    ),
    MOONSHINE_INTERNAL(
        "net{}kyori{}moonshine",
        "moonshine-internal",
        "2.0.4",
        "ChnFoz57fYxREjrmBZkHHodKP6VrFe70VWKUbn/QVII=",
        Relocation.of("kyori.moonshine", "net{}kyori{}moonshine")
    ),
    MOONSHINE_STANDARD(
        "net{}kyori{}moonshine",
        "moonshine-standard",
        "2.0.4",
        "tAqZea9MiMPsjZvXBIlVULvxBQlzGpHWSIgk+9yDGOE=",
        Relocation.of("kyori.moonshine", "net{}kyori{}moonshine"),
        Relocation.of("geantyref", "io{}leangen{}geantyref")
    ),
    MYSQL_DRIVER(
        "com{}mysql",
        "mysql-connector-j",
        "9.2.0",
        "fplBu9zKJE2Hjqlb//eI/Zumplr3V/JL5sYykw1hx+0=",
        Relocation.of("mysql", "com{}mysql")
    ),
    P6SPY(
        "p6spy",
        "p6spy",
        "3.9.1",
        "GXqmSmZ22leNrBCXgkrM2nqJ1ashwIKeBb/ekqF29KE=",
        Relocation.of("p6spy", "com{}p6spy{}engine")
    ),
    POSTGRES_DRIVER(
        "org.postgresql",
        "postgresql",
        "42.7.5",
        "aQILO9IJhFQ+gXOT8ubAGokO8uN6d90R1thQgYHQeas=",
        Relocation.of("postgresql", "org{}postgresql")
    ),
    QUARTZ(
        "org.quartz-scheduler",
        "quartz",
        "2.5.0",
        "7vPJGY4/NG6h2jhqC5QO1D15BM8hfsxXVfvn9HKfCAQ=",
        Relocation.of("quartz", "org{}quartz")
    ),
    // Jooq dep
    R2DBC(
        "io.r2dbc",
        "r2dbc-spi",
        "1.0.0.RELEASE",
        "pYRsWf6jNkMaSucsoU7b9SmbeEhvowjq+zg/SuDqdOU=",
        Relocation.of("r2dbc", "org{}r2dbc")
    ),
    // Jooq dep
    REACTIVE_STREAMS(
        "org.reactivestreams",
        "reactive-streams",
        "1.0.4",
        "91yll3ibPaxY9hhXuawuEDSmj6Zy2zUFWo+0UJ4yXyg=",
        Relocation.of("reactivestreams", "org{}reactivestreams")
    ),
    SQLITE_DRIVER(
        "org.xerial",
        "sqlite-jdbc",
        "3.49.1.0",
        "XIYJ0so0HeuMb3F3iXS1ukmVx9MtfHyJ2TkqPnLDkpE=",
        Relocation.of("sqlite", "org{}xerial")
    ),
    TASKCHAIN_BUKKIT(
        "co.aikar",
        "taskchain-bukkit",
        "3.7.2",
        "B/O3+zWGalLs8otAr8tdNnIc/39FDRh6tN5qvNgfEaI=",
        EnumSet.of(DependencyRepository.AIKAR),
        Relocation.of("taskchain", "co{}aikar{}taskchain")
    ),
    TASKCHAIN_CORE(
        "co.aikar",
        "taskchain-core",
        "3.7.2",
        "OpSCCN+7v6gqFpsU/LUNOOXzjImwjyE2ShHZ5xFUj/Q=",
        EnumSet.of(DependencyRepository.AIKAR),
        Relocation.of("taskchain", "co{}aikar{}taskchain")
    );

    private final String mavenRepoPath;
    private final String version;
    private final byte[] checksum;
    private final Set<DependencyRepository> repositories;
    private final List<Relocation> relocations;
    private static final String MAVEN_JAR_FORMAT = "%s-%s.jar";
    private static final String MAVEN_PATH_FORMAT = "%s/%s/%s/%s";

    Dependency(String groupId, String artifactId, String version, String checksum) {
        this(groupId, artifactId, version, checksum, new Relocation[0]);
    }

    Dependency(String groupId, String artifactId, String version, String checksum, Relocation... relocations) {
        this(
            groupId,
            artifactId,
            version,
            checksum,
            null,
            EnumSet.of(
                DependencyRepository.PRISM_MIRROR,
                DependencyRepository.LUCK_MIRROR,
                DependencyRepository.MAVEN_CENTRAL
            ),
            relocations
        );
    }

    Dependency(
        String groupId,
        String artifactId,
        String version,
        String checksum,
        Set<DependencyRepository> repositories,
        Relocation... relocations
    ) {
        this(groupId, artifactId, version, checksum, null, repositories, relocations);
    }

    Dependency(
        String groupId,
        String artifactId,
        String version,
        String checksum,
        String jarPath,
        Set<DependencyRepository> repositories,
        Relocation... relocations
    ) {
        if (jarPath == null) {
            jarPath = String.format(MAVEN_JAR_FORMAT, rewriteEscaping(artifactId), version);
        }

        this.mavenRepoPath = String.format(
            MAVEN_PATH_FORMAT,
            rewriteEscaping(groupId).replace(".", "/"),
            rewriteEscaping(artifactId),
            version,
            jarPath
        );
        this.version = version;
        this.checksum = Base64.getDecoder().decode(checksum);
        this.repositories = repositories;
        this.relocations = ImmutableList.copyOf(relocations);
    }

    /**
     * Rewrites escaping.
     * <p>Some patterns are escaped to avoid being touched by shade relocation. It might rewrite
     * the rules and break our custom remapping.</p>
     *
     * @param s The encoded package string
     * @return The java package
     */
    private static String rewriteEscaping(String s) {
        return s.replace("{}", ".");
    }

    /**
     * Format a filename.
     *
     * @param classifier The classifier (remapped or null)
     * @return The filename
     */
    public String fileName(String classifier) {
        String name = name().toLowerCase(Locale.ROOT).replace('_', '-');
        String extra = classifier == null || classifier.isEmpty() ? "" : "-" + classifier;

        return name + "-" + this.version + extra + ".jar";
    }

    /**
     * Get the maven repo path.
     *
     * @return The maven repo path
     */
    String mavenRepoPath() {
        return this.mavenRepoPath;
    }

    /**
     * Get the expected checksum.
     *
     * @return The checksum
     */
    public byte[] checksum() {
        return this.checksum;
    }

    /**
     * Compare checksums.
     *
     * @param hash The file hash
     * @return True if hashes match
     */
    public boolean checksumMatches(byte[] hash) {
        return Arrays.equals(this.checksum, hash);
    }

    /**
     * Get the repositories.
     *
     * @return The repositories
     */
    public Set<DependencyRepository> repositories() {
        return repositories;
    }

    /**
     * Get the relocations.
     *
     * @return The relocations
     */
    public List<Relocation> relocations() {
        return this.relocations;
    }

    /**
     * Creates a {@link MessageDigest} suitable for computing the checksums
     * of dependencies.
     *
     * @return the digest
     */
    public static MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
