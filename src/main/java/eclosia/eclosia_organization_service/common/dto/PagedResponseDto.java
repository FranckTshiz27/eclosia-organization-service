package eclosia.eclosia_organization_service.common.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class PagedResponseDto<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private int numberOfElements;
    private boolean empty;

    public static <T> PagedResponseDto<T> from(Page<T> pageData) {
        PagedResponseDto<T> response = new PagedResponseDto<>();
        response.setContent(pageData.getContent());
        response.setPage(pageData.getNumber());
        response.setSize(pageData.getSize());
        response.setTotalElements(pageData.getTotalElements());
        response.setTotalPages(pageData.getTotalPages());
        response.setFirst(pageData.isFirst());
        response.setLast(pageData.isLast());
        response.setNumberOfElements(pageData.getNumberOfElements());
        response.setEmpty(pageData.isEmpty());
        return response;
    }
}
