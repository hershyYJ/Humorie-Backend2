package com.example.humorie.consultant.counselor.service;

import com.example.humorie.account.entity.AccountDetail;
import com.example.humorie.account.jwt.JwtTokenUtil;
import com.example.humorie.account.repository.AccountRepository;
import com.example.humorie.consultant.counselor.dto.BookmarkDto;
import com.example.humorie.consultant.counselor.dto.CounselorDto;
import com.example.humorie.consultant.counselor.entity.Bookmark;
import com.example.humorie.consultant.counselor.entity.CounselingField;
import com.example.humorie.consultant.counselor.entity.Counselor;
import com.example.humorie.consultant.counselor.repository.BookmarkRepository;
import com.example.humorie.consultant.counselor.repository.CounselingFieldRepository;
import com.example.humorie.consultant.counselor.repository.CounselorRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final CounselorRepository counselorRepository;
    private final AccountRepository accountRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final CounselingFieldRepository fieldRepository;

    @Transactional
    public Bookmark addBookmark(String accessToken, long counselorId) {
        String email = jwtTokenUtil.getEmailFromToken(accessToken);

        AccountDetail account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Not found user"));

        Counselor counselor = counselorRepository.findById(counselorId)
                .orElseThrow(() -> new RuntimeException("Not found counselor"));

        if (bookmarkRepository.existsByAccountAndCounselor(account, counselor)) {
            throw new RuntimeException("Already bookmarked this counselor");
        }

        Bookmark bookmark = Bookmark.builder()
                .account(account)
                .counselor(counselor)
                .build();

        return bookmarkRepository.save(bookmark);
    }

    @Transactional
    public void removeBookmark(String accessToken, long counselorId) {
        String email = jwtTokenUtil.getEmailFromToken(accessToken);

        AccountDetail account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Not found user"));

        Counselor counselor = counselorRepository.findById(counselorId)
                .orElseThrow(() -> new RuntimeException("Not found counselor"));

        Bookmark bookmark = bookmarkRepository.findByAccountAndCounselor(account, counselor)
                .orElseThrow(() -> new RuntimeException("Bookmark not found"));


        bookmarkRepository.deleteByAccountAndCounselor(account, counselor);
    }

    @Transactional
    public List<BookmarkDto> getAllBookmarks(String accessToken) {
        String email = jwtTokenUtil.getEmailFromToken(accessToken);

        AccountDetail account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Bookmark> bookmarks = bookmarkRepository.findAllByAccount(account);

        return bookmarks.stream()
                .map(this::convertToBookmarkDto)
                .collect(Collectors.toList());
    }

    private BookmarkDto convertToBookmarkDto(Bookmark bookmark) {
        BookmarkDto bookmarkDto = new BookmarkDto();
        bookmarkDto.setBookmarkId(bookmark.getId());
        bookmarkDto.setCreatedAt(bookmark.getCreatedAt());

        CounselorDto counselorDto = convertToCounselorDto(bookmark.getCounselor());
        bookmarkDto.setCounselor(counselorDto);

        return bookmarkDto;
    }

    private CounselorDto convertToCounselorDto(Counselor counselor) {
        CounselorDto counselorDto = new CounselorDto();

        counselorDto.setCounselorId(counselor.getId());
        counselorDto.setName(counselor.getName());

        Set<String> counselingFields = fieldRepository.findByCounselorId(counselor.getId()).stream()
                .map(CounselingField::getField)
                .collect(Collectors.toSet());
        counselorDto.setCounselingFields(counselingFields);

        return counselorDto;
    }

}
