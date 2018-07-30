package android.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.runner.BaseTestRunner;

@Deprecated
public class TestCaseUtil {
    private TestCaseUtil() {
    }

    public static List<? extends Test> getTests(Test test, boolean flatten) {
        return getTests(test, flatten, new HashSet());
    }

    private static List<? extends Test> getTests(Test test, boolean flatten, Set<Class<?>> seen) {
        List<Test> testCases = new ArrayList();
        if (test != null) {
            TestSuite workingTest = null;
            if ((test instanceof TestCase) && ((TestCase) test).getName() == null) {
                workingTest = invokeSuiteMethodIfPossible(test.getClass(), seen);
            }
            if (workingTest == null) {
                workingTest = test;
            }
            if (workingTest instanceof TestSuite) {
                Enumeration enumeration = workingTest.tests();
                while (enumeration.hasMoreElements()) {
                    Test childTest = (Test) enumeration.nextElement();
                    if (flatten) {
                        testCases.addAll(getTests(childTest, flatten, seen));
                    } else {
                        testCases.add(childTest);
                    }
                }
            } else {
                testCases.add(workingTest);
            }
        }
        return testCases;
    }

    static Test invokeSuiteMethodIfPossible(Class testClass, Set<Class<?>> seen) {
        try {
            Method suiteMethod = testClass.getMethod(BaseTestRunner.SUITE_METHODNAME, new Class[0]);
            if (Modifier.isStatic(suiteMethod.getModifiers()) && (seen.contains(testClass) ^ 1) != 0) {
                seen.add(testClass);
                try {
                    return (Test) suiteMethod.invoke(null, (Object[]) null);
                } catch (InvocationTargetException e) {
                } catch (IllegalAccessException e2) {
                }
            }
        } catch (NoSuchMethodException e3) {
        }
        return null;
    }

    static String getTestName(Test test) {
        if (test instanceof TestCase) {
            return ((TestCase) test).getName();
        }
        if (test instanceof TestSuite) {
            String name = ((TestSuite) test).getName();
            if (name != null) {
                int index = name.lastIndexOf(".");
                if (index > -1) {
                    return name.substring(index + 1);
                }
                return name;
            }
        }
        return "";
    }
}
