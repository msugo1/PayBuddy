import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { useSearchParams } from 'next/navigation';
import ThreeDSPage from './page';

// Mock next/navigation
vi.mock('next/navigation', () => ({
  useSearchParams: vi.fn(),
}));

type MockSearchParams = {
  get: (key: string) => string | null;
};

describe('ThreeDSPage', () => {
  const mockUseSearchParams = vi.mocked(useSearchParams);
  let mockFormSubmit: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();

    // Mock form submit
    mockFormSubmit = vi.fn();
    HTMLFormElement.prototype.submit = mockFormSubmit;
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should show error when required parameters are missing', () => {
    mockUseSearchParams.mockReturnValue({
      get: () => null,
    } as MockSearchParams);

    render(<ThreeDSPage />);

    expect(screen.getByText('잘못된 접근입니다')).toBeInTheDocument();
    expect(screen.getByText('3DS 인증 정보가 없습니다')).toBeInTheDocument();
  });

  it('should show error when acsUrl is missing', () => {
    mockUseSearchParams.mockReturnValue({
      get: (key: string) => {
        if (key === 'creq') return 'test-creq';
        if (key === 'paymentKey') return 'pay_test123';
        return null;
      },
    } as MockSearchParams);

    render(<ThreeDSPage />);

    expect(screen.getByText('잘못된 접근입니다')).toBeInTheDocument();
  });

  it('should show error when creq is missing', () => {
    mockUseSearchParams.mockReturnValue({
      get: (key: string) => {
        if (key === 'acsUrl') return 'https://acs.example.com';
        if (key === 'paymentKey') return 'pay_test123';
        return null;
      },
    } as MockSearchParams);

    render(<ThreeDSPage />);

    expect(screen.getByText('잘못된 접근입니다')).toBeInTheDocument();
  });

  it('should show error when paymentKey is missing', () => {
    mockUseSearchParams.mockReturnValue({
      get: (key: string) => {
        if (key === 'acsUrl') return 'https://acs.example.com';
        if (key === 'creq') return 'test-creq';
        return null;
      },
    } as MockSearchParams);

    render(<ThreeDSPage />);

    expect(screen.getByText('잘못된 접근입니다')).toBeInTheDocument();
  });

  it('should render 3DS challenge page with all parameters', () => {
    mockUseSearchParams.mockReturnValue({
      get: (key: string) => {
        if (key === 'acsUrl') return 'https://acs.example.com';
        if (key === 'creq') return 'test-creq';
        if (key === 'size') return '02';
        if (key === 'paymentKey') return 'pay_test123';
        return null;
      },
    } as MockSearchParams);

    render(<ThreeDSPage />);

    expect(screen.getByText('3DS 인증')).toBeInTheDocument();
    expect(screen.getByText('카드사 인증을 진행해주세요')).toBeInTheDocument();
    expect(screen.getByTitle('3DS Challenge')).toBeInTheDocument();
  });

  it('should auto-submit form to ACS URL', () => {
    mockUseSearchParams.mockReturnValue({
      get: (key: string) => {
        if (key === 'acsUrl') return 'https://acs.example.com';
        if (key === 'creq') return 'test-creq';
        if (key === 'paymentKey') return 'pay_test123';
        return null;
      },
    } as MockSearchParams);

    render(<ThreeDSPage />);

    expect(mockFormSubmit).toHaveBeenCalled();
  });

  it('should use default size 02 when size is not provided', () => {
    mockUseSearchParams.mockReturnValue({
      get: (key: string) => {
        if (key === 'acsUrl') return 'https://acs.example.com';
        if (key === 'creq') return 'test-creq';
        if (key === 'paymentKey') return 'pay_test123';
        return null; // No size
      },
    } as MockSearchParams);

    render(<ThreeDSPage />);

    const iframe = screen.getByTitle('3DS Challenge') as HTMLIFrameElement;
    expect(iframe.style.width).toBe('390px');
    expect(iframe.style.height).toBe('400px');
  });

  it('should apply size 01 (250x400)', () => {
    mockUseSearchParams.mockReturnValue({
      get: (key: string) => {
        if (key === 'acsUrl') return 'https://acs.example.com';
        if (key === 'creq') return 'test-creq';
        if (key === 'size') return '01';
        if (key === 'paymentKey') return 'pay_test123';
        return null;
      },
    } as MockSearchParams);

    render(<ThreeDSPage />);

    const iframe = screen.getByTitle('3DS Challenge') as HTMLIFrameElement;
    expect(iframe.style.width).toBe('250px');
    expect(iframe.style.height).toBe('400px');
  });

  it('should apply size 03 (500x600)', () => {
    mockUseSearchParams.mockReturnValue({
      get: (key: string) => {
        if (key === 'acsUrl') return 'https://acs.example.com';
        if (key === 'creq') return 'test-creq';
        if (key === 'size') return '03';
        if (key === 'paymentKey') return 'pay_test123';
        return null;
      },
    } as MockSearchParams);

    render(<ThreeDSPage />);

    const iframe = screen.getByTitle('3DS Challenge') as HTMLIFrameElement;
    expect(iframe.style.width).toBe('500px');
    expect(iframe.style.height).toBe('600px');
  });

  it('should apply size 04 (600x400)', () => {
    mockUseSearchParams.mockReturnValue({
      get: (key: string) => {
        if (key === 'acsUrl') return 'https://acs.example.com';
        if (key === 'creq') return 'test-creq';
        if (key === 'size') return '04';
        if (key === 'paymentKey') return 'pay_test123';
        return null;
      },
    } as MockSearchParams);

    render(<ThreeDSPage />);

    const iframe = screen.getByTitle('3DS Challenge') as HTMLIFrameElement;
    expect(iframe.style.width).toBe('600px');
    expect(iframe.style.height).toBe('400px');
  });

  it('should apply size 05 (fullscreen)', () => {
    mockUseSearchParams.mockReturnValue({
      get: (key: string) => {
        if (key === 'acsUrl') return 'https://acs.example.com';
        if (key === 'creq') return 'test-creq';
        if (key === 'size') return '05';
        if (key === 'paymentKey') return 'pay_test123';
        return null;
      },
    } as MockSearchParams);

    render(<ThreeDSPage />);

    const iframe = screen.getByTitle('3DS Challenge') as HTMLIFrameElement;
    expect(iframe.style.width).toBe('100%');
    expect(iframe.style.height).toBe('100vh');
  });

  it.skip('should show timeout message after 3 minutes', async () => {
    mockUseSearchParams.mockReturnValue({
      get: (key: string) => {
        if (key === 'acsUrl') return 'https://acs.example.com';
        if (key === 'creq') return 'test-creq';
        if (key === 'paymentKey') return 'pay_test123';
        return null;
      },
    } as MockSearchParams);

    const { rerender } = render(<ThreeDSPage />);

    expect(screen.getByText('3DS 인증')).toBeInTheDocument();

    // Fast-forward time
    vi.runAllTimers();

    // Force re-render
    rerender(<ThreeDSPage />);

    await waitFor(
      () => {
        expect(screen.getByText('인증 시간 초과')).toBeInTheDocument();
      },
      { timeout: 1000 }
    );

    expect(screen.getByText(/3DS 인증 시간이 초과되었습니다/)).toBeInTheDocument();
  });

  it('should not timeout if component unmounts before timeout', async () => {
    mockUseSearchParams.mockReturnValue({
      get: (key: string) => {
        if (key === 'acsUrl') return 'https://acs.example.com';
        if (key === 'creq') return 'test-creq';
        if (key === 'paymentKey') return 'pay_test123';
        return null;
      },
    } as MockSearchParams);

    const { unmount } = render(<ThreeDSPage />);

    expect(screen.getByText('3DS 인증')).toBeInTheDocument();

    // Unmount before timeout
    unmount();

    // Fast-forward 3 minutes
    vi.advanceTimersByTime(180000);

    // Should not crash or show error
  });

  it('should display timeout duration in minutes', () => {
    mockUseSearchParams.mockReturnValue({
      get: (key: string) => {
        if (key === 'acsUrl') return 'https://acs.example.com';
        if (key === 'creq') return 'test-creq';
        if (key === 'paymentKey') return 'pay_test123';
        return null;
      },
    } as MockSearchParams);

    render(<ThreeDSPage />);

    expect(screen.getByText(/최대 3분 이내 완료해주세요/)).toBeInTheDocument();
  });

  it('should create hidden form with correct attributes', () => {
    mockUseSearchParams.mockReturnValue({
      get: (key: string) => {
        if (key === 'acsUrl') return 'https://acs.example.com';
        if (key === 'creq') return 'test-creq-value';
        if (key === 'paymentKey') return 'pay_test123';
        return null;
      },
    } as MockSearchParams);

    const { container } = render(<ThreeDSPage />);

    const form = container.querySelector('form');
    expect(form).toBeInTheDocument();
    expect(form?.method).toBe('post');
    expect(form?.action).toBe('https://acs.example.com/');
    expect(form?.target).toBe('threeds-iframe');
    expect(form?.style.display).toBe('none');

    const input = form?.querySelector('input[name="creq"]') as HTMLInputElement;
    expect(input).toBeInTheDocument();
    expect(input?.value).toBe('test-creq-value');
  });
});
