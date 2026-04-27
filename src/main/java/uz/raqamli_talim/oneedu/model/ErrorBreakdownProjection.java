
package uz.raqamli_talim.oneedu.model;

public interface ErrorBreakdownProjection {
    String getErrorMessage();
    Long getCount();
    Double getPercentage();
}