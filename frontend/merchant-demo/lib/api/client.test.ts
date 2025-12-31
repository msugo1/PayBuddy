import { describe, it, expect } from 'vitest';
import { http, HttpResponse } from 'msw';
import { server } from '../mocks/server';
import { apiClient } from './client';
import { ApiError } from './types';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/v1';

describe('apiClient', () => {
  describe('Error Handling', () => {
    it('should throw ApiError when response is Problem JSON', async () => {
      server.use(
        http.get(`${API_BASE_URL}/test-error`, () => {
          return HttpResponse.json(
            {
              type: 'about:blank',
              title: 'Bad Request',
              status: 400,
              detail: 'Test error message',
              error_code: 'TEST_ERROR',
            },
            {
              status: 400,
              headers: {
                'Content-Type': 'application/problem+json',
              },
            }
          );
        })
      );

      try {
        await apiClient.get('test-error');
        expect.fail('Should have thrown ApiError');
      } catch (error) {
        expect(error).toBeInstanceOf(ApiError);
        if (error instanceof ApiError) {
          expect(error.status).toBe(400);
          expect(error.errorResponse.title).toBe('Bad Request');
          expect(error.errorResponse.error_code).toBe('TEST_ERROR');
        }
      }
    });

    it('should pass through non-Problem JSON errors', async () => {
      server.use(
        http.get(`${API_BASE_URL}/test-non-problem-error`, () => {
          return HttpResponse.json(
            { message: 'Regular error' },
            { status: 500 }
          );
        })
      );

      try {
        await apiClient.get('test-non-problem-error').json();
        expect.fail('Should have thrown error');
      } catch (error) {
        // Should NOT be ApiError
        expect(error).not.toBeInstanceOf(ApiError);
      }
    });
  });

  describe('Retry Logic', () => {
    it('should retry on 500 errors', async () => {
      let attemptCount = 0;
      server.use(
        http.get(`${API_BASE_URL}/test-retry`, () => {
          attemptCount++;
          if (attemptCount < 2) {
            return HttpResponse.json({}, { status: 500 });
          }
          return HttpResponse.json({ success: true }, { status: 200 });
        })
      );

      const response = await apiClient.get('test-retry');
      const data = await response.json();

      expect(data).toEqual({ success: true });
      expect(attemptCount).toBeGreaterThanOrEqual(2);
    });
  });

  describe('Headers', () => {
    it('should set Content-Type header to application/json', async () => {
      let receivedContentType: string | null = null;

      server.use(
        http.post(`${API_BASE_URL}/test-headers`, async ({ request }) => {
          receivedContentType = request.headers.get('content-type');
          return HttpResponse.json({ ok: true });
        })
      );

      await apiClient.post('test-headers', { json: { test: 'data' } });

      expect(receivedContentType).toBe('application/json');
    });
  });
});
