import { NextRequest, NextResponse } from 'next/server';

export async function POST(request: NextRequest) {
  // POST body에서 creq 추출
  const formData = await request.formData();
  const creq = formData.get('creq') as string;

  if (!creq) {
    return NextResponse.json(
      { error: 'creq is required' },
      { status: 400 }
    );
  }

  // 쿼리 파라미터로 변환해서 challenge 페이지로 리다이렉트
  const url = new URL('/mock-acs/challenge', request.url);
  url.searchParams.set('creq', creq);

  return NextResponse.redirect(url);
}

// GET 요청도 지원 (테스트용)
export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const creq = searchParams.get('creq');

  if (!creq) {
    return NextResponse.json(
      { error: 'creq is required' },
      { status: 400 }
    );
  }

  // challenge 페이지로 리다이렉트
  const url = new URL('/mock-acs/challenge', request.url);
  url.searchParams.set('creq', creq);

  return NextResponse.redirect(url);
}
