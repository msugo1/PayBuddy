import ky from 'ky';
import { ApiError, ErrorResponse } from './types';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/v1';

export const apiClient = ky.create({
  prefixUrl: API_BASE_URL,
  timeout: 30000,
  retry: {
    limit: 2,
    statusCodes: [408, 500, 502, 503, 504],
  },
  hooks: {
    beforeRequest: [
      (request) => {
        // TODO: Add authentication headers if needed
        request.headers.set('Content-Type', 'application/json');
      },
    ],
    afterResponse: [
      async (_request, _options, response) => {
        if (!response.ok) {
          const contentType = response.headers.get('content-type');
          if (contentType?.includes('application/problem+json')) {
            const errorResponse = (await response.json()) as ErrorResponse;
            throw new ApiError(response.status, errorResponse);
          }
        }
        return response;
      },
    ],
  },
});
