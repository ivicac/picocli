package picocli;

import org.junit.*;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TestRule;
import org.junit.Test;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;

public class CommandSubcommandFilterTest {
    // allows tests to set any kind of properties they like, without having to individually roll them back
    @Rule
    public final TestRule restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final ProvideSystemProperty ansiOFF = new ProvideSystemProperty("picocli.ansi", "false");

    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog().muteForSuccessfulTests();

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog().muteForSuccessfulTests();

    @CommandLine.Command(name = "aa")
    private static class AA implements Callable<Integer> {
        public Integer call() {
            return 0;
        }
    }


    @CommandLine.Command(name = "c")
    private static class C implements Callable<Integer> {
        public Integer call() {
            return 0;
        }
    }

    @CommandLine.Command(name = "b")
    private static class B implements Callable<Integer> {
        public Integer call() {
            return 0;
        }
    }


    @CommandLine.Command(name = "a", subcommands = {AA.class}, preprocessor = A.AFilter.class)
    private static class A implements Callable<Integer> {

        private static class AFilter implements CommandLine.IPreprocessor {
            public CommandLine.Model.CommandSpec preprocess(CommandLine.Model.CommandSpec commandSpec) {
                // verify it's context aware
                assertEquals(2, commandSpec.subcommands().size());

                if (Boolean.valueOf(System.getProperty("disable_aa"))) {
                    commandSpec.removeSubcommand("aa");
                }
                if (Boolean.valueOf(System.getProperty("disable_ab"))) {
                    commandSpec.removeSubcommand("ab");
                }
                return commandSpec;
            }
        }

        public Integer call() {
            return 0;
        }

        @CommandLine.Command(name = "ab")
        private int ab()
        {
            return 0;
        }
    }


    @CommandLine.Command(name="inital", subcommands = {A.class, B.class, C.class}, preprocessor = Main.MainFilter.class)
    private static class Main implements Callable<Integer> {

        private static class MainFilter implements CommandLine.IPreprocessor {
            public CommandLine.Model.CommandSpec preprocess(CommandLine.Model.CommandSpec commandSpec) {
                // verify it's context aware
                assertEquals(3, commandSpec.subcommands().size());

                if (Boolean.valueOf(System.getProperty("disable_a"))) {
                    commandSpec.removeSubcommand("a");
                }
                if (Boolean.valueOf(System.getProperty("disable_c"))) {
                    commandSpec.removeSubcommand("c");
                }
                return commandSpec;
            }
        }

        public Integer call() {
            return 0;
        }
    }

    @Test
    public void testMainFiltering() {
        System.setProperty("disable_a", "true");
        System.setProperty("disable_c", "true");

        CommandLine cmd = new CommandLine(new Main());

        assertEquals(1, cmd.getSubcommands().size());
        assertArrayEquals(new String[]{"b"}, cmd.getSubcommands().keySet().toArray(new String[0]));
    }

    @Test
    public void testNestedSubcommandFiltering() {
        System.setProperty("disable_aa", "true");

        CommandLine cmd = new CommandLine(new Main());

        // there is only function annotated subcommand now
        assertArrayEquals(new String[]{"ab"}, cmd.getSubcommands().get("a").getSubcommands().keySet().toArray(new String[0]));
    }

    @Test
    public void testFunctionAnnotatedCommandFiltering() {
        System.setProperty("disable_ab", "true");

        CommandLine cmd = new CommandLine(new Main());

        // there is only class annotated subcommand now
        assertArrayEquals(new String[]{"aa"}, cmd.getSubcommands().get("a").getSubcommands().keySet().toArray(new String[0]));
    }
}


