package java.lang.reflect;

public interface WildcardType extends Type {
    Type[] getLowerBounds();

    Type[] getUpperBounds();
}
