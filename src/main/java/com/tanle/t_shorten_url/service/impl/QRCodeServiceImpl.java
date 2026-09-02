package com.tanle.t_shorten_url.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.tanle.t_shorten_url.service.QRCodeService;
import com.tanle.t_shorten_url.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class QRCodeServiceImpl implements QRCodeService {
    private final UploadService uploadService;


    @Override
    public String generateQrCode(String code) {
        try {
            final int WIDTH = 300;
            final int HEIGHT = 300;
            final String FORMAT = "PNG";
            final String CONTENT_TYPE = "image/png";
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    code,
                    BarcodeFormat.QR_CODE,
                    WIDTH,
                    HEIGHT
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(
                    bitMatrix,
                    FORMAT,
                    outputStream
            );
            String[] lastCode = code.split("/");
            return uploadService.uploadFile(lastCode[lastCode.length - 1], outputStream.toByteArray(), CONTENT_TYPE);
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        } catch (WriterException writerException) {
            throw new RuntimeException(writerException);
        }
    }

}
