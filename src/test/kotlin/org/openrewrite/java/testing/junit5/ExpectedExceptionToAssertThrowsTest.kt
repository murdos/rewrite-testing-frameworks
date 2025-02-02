/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.testing.junit5

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class ExpectedExceptionToAssertThrowsTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .classpath("junit", "hamcrest")
        .build()

    override val recipe = ExpectedExceptionToAssertThrows()

    @Test
    fun leavesOtherRulesAlone() = assertChanged(
        before = """
            import org.junit.Rule;
            import org.junit.rules.TemporaryFolder;
            import org.junit.rules.ExpectedException;

            class A {
            
                @Rule
                TemporaryFolder tempDir = new TemporaryFolder();

                @Rule
                ExpectedException thrown = ExpectedException.none();
            }
        """,
        after = """
            import org.junit.Rule;
            import org.junit.rules.TemporaryFolder;

            class A {

                @Rule
                TemporaryFolder tempDir = new TemporaryFolder();
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite-testing-frameworks/issues/72")
    @Test
    fun removeExpectedExceptionAndLeaveMethodAlone() = assertChanged(
        before = """
            package org.openrewrite.java.testing.junit5;

            import org.junit.Rule;
            import org.junit.rules.ExpectedException;
            
            public class SimpleExpectedExceptionTest {
                @Rule
                public ExpectedException thrown = ExpectedException.none();
            
                public void doNotChange() {
                    final String noChanges = "atAll";
                }
            }
        """,
        after = """
            package org.openrewrite.java.testing.junit5;

            public class SimpleExpectedExceptionTest {
            
                public void doNotChange() {
                    final String noChanges = "atAll";
                }
            }
        """
    )

    @Test
    fun refactorExceptClass() = assertChanged(
        before = """
            package org.openrewrite.java.testing.junit5;

            import org.junit.Rule;
            import org.junit.rules.ExpectedException;
            
            public class SimpleExpectedExceptionTest {
                @Rule
                public ExpectedException thrown = ExpectedException.none();
            
                public void throwsExceptionWithSpecificType() {
                    thrown.expect(NullPointerException.class);
                    throw new NullPointerException();
                }
            }
        """,
        after = """
            package org.openrewrite.java.testing.junit5;
            
            import static org.junit.jupiter.api.Assertions.assertThrows;
            
            public class SimpleExpectedExceptionTest {
            
                public void throwsExceptionWithSpecificType() {
                    assertThrows(NullPointerException.class, () -> {
                        throw new NullPointerException();
                    });
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite-testing-frameworks/issues/72")
    @Test
    fun refactorExceptWithMatcher() = assertChanged(
        before = """
            package org.openrewrite.java.testing.junit5;

            import org.junit.Rule;
            import org.junit.rules.ExpectedException;
            
            import static org.hamcrest.Matchers.isA; 

            public class SimpleExpectedExceptionTest {
                @Rule
                public ExpectedException thrown = ExpectedException.none();
            
                public void throwsExceptionWithSpecificType() {
                    thrown.expect(isA(NullPointerException.class));
                    throw new NullPointerException();
                }
            }
        """,
        after = """
            package org.openrewrite.java.testing.junit5;
            
            import static org.hamcrest.MatcherAssert.assertThat;
            import static org.hamcrest.Matchers.isA;
            import static org.junit.jupiter.api.Assertions.assertThrows;
            
            public class SimpleExpectedExceptionTest {
            
                public void throwsExceptionWithSpecificType() {
                    Exception exception = assertThrows(Exception.class, () -> {
                        throw new NullPointerException();
                    });
                    assertThat(exception, isA(NullPointerException.class));
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite-testing-frameworks/issues/77")
    @Test
    fun refactorExpectMessageString() = assertChanged(
        before = """
            package org.openrewrite.java.testing.junit5;
            
            import org.junit.Rule;
            import org.junit.rules.ExpectedException;
            
            public class SimpleExpectedExceptionTest {
                @Rule
                public ExpectedException thrown = ExpectedException.none();
            
                public void statementsBeforeExpected() {
                    int[] a = new int[] { 1 };
                    thrown.expect(IndexOutOfBoundsException.class);
                    thrown.expectMessage("Index 1 out of bounds for length 1");
                    int b = a[1];
                }
            }
        """,
        after = """
            package org.openrewrite.java.testing.junit5;
            
            import static org.junit.jupiter.api.Assertions.assertThrows;
            
            public class SimpleExpectedExceptionTest {
            
                public void statementsBeforeExpected() {
                    assertThrows(IndexOutOfBoundsException.class, () -> {
                        int[] a = new int[]{1};
                        int b = a[1];
                    }, "Index 1 out of bounds for length 1");
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite-testing-frameworks/issues/72")
    @Test
    fun refactorExpectMessageWithMatcher() = assertChanged(
        before = """
            package org.openrewrite.java.testing.junit5;
            
            import org.junit.Rule;
            import org.junit.rules.ExpectedException;

            import static org.hamcrest.Matchers.containsString;
            
            public class ExampleTests {
                @Rule
                public ExpectedException thrown = ExpectedException.none();
            
                public void expectMessageWithMatcher() {
                    this.thrown.expectMessage(containsString("rewrite expectMessage"));
                    throw new NullPointerException("rewrite expectMessage with hamcrest matcher.");
                }
            }
        """,
        after = """
            package org.openrewrite.java.testing.junit5;
            
            import static org.hamcrest.MatcherAssert.assertThat;
            import static org.hamcrest.Matchers.containsString;
            import static org.junit.jupiter.api.Assertions.assertThrows;
            
            public class ExampleTests {
            
                public void expectMessageWithMatcher() {
                    Exception exception = assertThrows(Exception.class, () -> {
                        throw new NullPointerException("rewrite expectMessage with hamcrest matcher.");
                    });
                    assertThat(exception.getMessage(), containsString("rewrite expectMessage"));
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite-testing-frameworks/issues/72")
    @Test
    fun refactorExpectCauseWithMatchers() = assertChanged(
        before = """
            package org.openrewrite.java.testing.junit5;
            
            import org.junit.Rule;
            import org.junit.rules.ExpectedException;

            import static org.hamcrest.Matchers.nullValue;
            
            public class ExampleTests {
                @Rule
                public ExpectedException thrown = ExpectedException.none();
            
                public void expectCause() {
                    this.thrown.expectCause(nullValue());
                    throw new NullPointerException("rewrite expectMessage with hamcrest matcher.");
                }
            }
        """,
        after = """
            package org.openrewrite.java.testing.junit5;
            
            import static org.hamcrest.MatcherAssert.assertThat;
            import static org.hamcrest.Matchers.nullValue;
            import static org.junit.jupiter.api.Assertions.assertThrows;
            
            public class ExampleTests {
            
                public void expectCause() {
                    Exception exception = assertThrows(Exception.class, () -> {
                        throw new NullPointerException("rewrite expectMessage with hamcrest matcher.");
                    });
                    assertThat(exception.getCause(), nullValue());
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite-testing-frameworks/issues/72")
    @Test
    fun refactorExpectException() = assertChanged(
        before = """
            package org.openrewrite.java.testing.junit5;
            
            import org.junit.Rule;
            import org.junit.rules.ExpectedException;

            import static org.hamcrest.Matchers.*;
            
            public class ExampleTests {
                @Rule
                public ExpectedException thrown = ExpectedException.none();
            
                public void expectExceptionUseCases() {
                    this.thrown.expect(isA(NullPointerException.class));
                    this.thrown.expectMessage(containsString("rewrite expectMessage"));
                    this.thrown.expectCause(nullValue());
                    throw new NullPointerException("rewrite expectMessage with hamcrest matcher.");
                }
            }
        """,
        after = """
            package org.openrewrite.java.testing.junit5;
            
            import static org.hamcrest.MatcherAssert.assertThat;
            import static org.hamcrest.Matchers.*;
            import static org.junit.jupiter.api.Assertions.assertThrows;
            
            public class ExampleTests {
            
                public void expectExceptionUseCases() {
                    Exception exception = assertThrows(Exception.class, () -> {
                        throw new NullPointerException("rewrite expectMessage with hamcrest matcher.");
                    });
                    assertThat(exception, isA(NullPointerException.class));
                    assertThat(exception.getMessage(), containsString("rewrite expectMessage"));
                    assertThat(exception.getCause(), nullValue());
                }
            }
        """
    )
}
