const STUDENT_PAGES = [
  { route: '/student/dashboard', matches: (pathname) => pathname === '/student/dashboard' },
  { route: '/student/class', matches: (pathname) => pathname === '/student/class' },
  { route: '/student/projects', matches: (pathname) => pathname.startsWith('/student/projects') },
  { route: '/student/leetcode', matches: (pathname) => pathname === '/student/leetcode' },
];

export function getStudentSwipeDestination(pathname, deltaX, deltaY, threshold = 60) {
  if (Math.abs(deltaX) < threshold || Math.abs(deltaX) <= Math.abs(deltaY) * 1.25) return null;

  const currentIndex = STUDENT_PAGES.findIndex(({ matches }) => matches(pathname));
  if (currentIndex < 0) return null;

  const nextIndex = currentIndex + (deltaX < 0 ? 1 : -1);
  return STUDENT_PAGES[nextIndex]?.route ?? null;
}

export function isSwipeExcludedTarget(target) {
  if (!(target instanceof Element)) return true;
  if (
    target.closest(
      'a, button, input, select, textarea, label, form, summary, iframe, [role="button"], [role="link"], [role="menuitem"], [role="tab"], [role="dialog"], [role="listbox"], [role="option"], [role="combobox"], [role="checkbox"], [role="radio"], [role="switch"], [role="slider"], [role="spinbutton"], [aria-haspopup], [tabindex]:not([tabindex="-1"]), [contenteditable="true"], [data-no-swipe], .field, .modal, .modal-backdrop',
    )
  ) {
    return true;
  }

  for (let element = target; element; element = element.parentElement) {
    if (element.scrollWidth > element.clientWidth) {
      const { overflowX } = window.getComputedStyle(element);
      if (overflowX === 'auto' || overflowX === 'scroll') return true;
    }
  }
  return false;
}
