import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import Sidebar from './Sidebar';
import Topbar from './Topbar';
import MobileBottomNavigation from './MobileBottomNavigation';
import MobileQuickActions from './MobileQuickActions';
import {
  getStudentSwipeDestination,
  isSwipeExcludedTarget,
} from '../../utils/mobileSwipeNavigation';

export default function AppLayout() {
  const { user, role, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const stageRef = useRef(null);
  const pageRef = useRef(null);
  const touchStart = useRef(null);
  const transitionRef = useRef(null);
  const frameRef = useRef(null);
  const timeoutRef = useRef(null);

  const isMobile = () => window.matchMedia('(max-width: 760px)').matches;
  const reduceMotion = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const cancelFrame = () => {
    if (frameRef.current !== null) cancelAnimationFrame(frameRef.current);
    frameRef.current = null;
  };

  const updateDragPosition = (transition, deltaX) => {
    if (!transition.ghost.isConnected || !pageRef.current) return;
    const movingTowardDestination = -deltaX * transition.direction > 0;
    const distance = movingTowardDestination
      ? Math.max(-transition.width, Math.min(transition.width, deltaX))
      : 0;
    const incoming = transition.direction * (transition.width - Math.abs(distance));
    transition.ghost.style.transform = `translate3d(${distance}px, 0, 0)`;
    pageRef.current.style.transform = `translate3d(${incoming}px, 0, 0)`;
  };

  const finishTransition = (transition) => {
    if (transitionRef.current !== transition) return;
    if (transition.rollback) {
      transition.phase = 'returning';
      navigate(`${transition.origin}${transition.originSearch}`, { replace: true });
      return;
    }
    transition.ghost.remove();
    pageRef.current?.style.removeProperty('transform');
    pageRef.current?.style.removeProperty('transition');
    pageRef.current?.classList.remove('mobile-transition-active');
    transitionRef.current = null;
  };

  const runSettledAnimation = (transition) => {
    cancelFrame();
    transition.phase = 'settling';
    const ghostTarget = transition.rollback ? 0 : -transition.direction * transition.width;
    const pageTarget = transition.rollback ? transition.direction * transition.width : 0;
    const duration = reduceMotion() ? 1 : 300;

    transition.ghost.style.transition = `transform ${duration}ms cubic-bezier(0.22, 1, 0.36, 1)`;
    if (pageRef.current) {
      pageRef.current.style.transition = `transform ${duration}ms cubic-bezier(0.22, 1, 0.36, 1)`;
    }
    frameRef.current = requestAnimationFrame(() => {
      frameRef.current = null;
      if (transitionRef.current !== transition) return;
      transition.ghost.style.transform = `translate3d(${ghostTarget}px, 0, 0)`;
      if (pageRef.current) pageRef.current.style.transform = `translate3d(${pageTarget}px, 0, 0)`;
    });
    timeoutRef.current = window.setTimeout(() => finishTransition(transition), duration + 40);
  };

  const settleTransition = (transition, rollback) => {
    if (transitionRef.current !== transition || transition.phase === 'settling') return;
    transition.rollback = rollback;
    if (!transition.ready) {
      transition.phase = 'waiting-settle';
      return;
    }
    runSettledAnimation(transition);
  };

  const startTransition = (to, direction, mode = 'tap', offset = 0) => {
    if (!isMobile() || reduceMotion() || !stageRef.current || !pageRef.current) return false;
    if (transitionRef.current) return true;
    const destinationUrl = new URL(to, window.location.origin);
    if (`${destinationUrl.pathname}${destinationUrl.search}` === `${location.pathname}${location.search}`) return false;

    const width = stageRef.current.clientWidth;
    const ghost = pageRef.current.cloneNode(true);
    ghost.classList.add('mobile-transition-ghost');
    pageRef.current.classList.add('mobile-transition-active');
    ghost.setAttribute('aria-hidden', 'true');
    ghost.inert = true;
    ghost.removeAttribute('id');
    ghost.querySelectorAll('[id]').forEach((element) => element.removeAttribute('id'));
    ghost.style.width = `${width}px`;
    ghost.style.transform = `translate3d(${offset}px, 0, 0)`;
    stageRef.current.append(ghost);

    const transition = {
      origin: location.pathname,
      originSearch: location.search,
      destination: destinationUrl.pathname,
      destinationSearch: destinationUrl.search,
      direction,
      width,
      ghost,
      phase: mode,
      offset,
      ready: false,
      rollback: false,
    };
    transitionRef.current = transition;
    navigate(to);
    return true;
  };

  const handleBottomNavigation = (to, direction) => startTransition(to, direction);

  useLayoutEffect(() => {
    const transition = transitionRef.current;
    if (!transition) return;
    if (
      transition.phase === 'returning' &&
      location.pathname === transition.origin &&
      location.search === transition.originSearch
    ) {
      transition.ghost.remove();
      pageRef.current?.style.removeProperty('transform');
      pageRef.current?.style.removeProperty('transition');
      pageRef.current?.classList.remove('mobile-transition-active');
      transitionRef.current = null;
      if (timeoutRef.current) window.clearTimeout(timeoutRef.current);
      cancelFrame();
      return;
    }
    if (
      location.pathname !== transition.destination ||
      location.search !== transition.destinationSearch ||
      !pageRef.current
    ) {
      return;
    }

    transition.ready = true;
    pageRef.current.style.transition = 'none';
    pageRef.current.classList.add('mobile-transition-active');
    if (transition.phase === 'tap' || transition.phase === 'waiting-settle') {
      const startOffset = transition.phase === 'tap' ? 0 : transition.offset;
      updateDragPosition(transition, startOffset);
      if (transition.phase === 'tap') {
        pageRef.current.style.transform = `translate3d(${transition.direction * transition.width}px, 0, 0)`;
        transition.ghost.style.transform = 'translate3d(0, 0, 0)';
      }
      frameRef.current = requestAnimationFrame(() => {
        frameRef.current = null;
        if (transitionRef.current !== transition) return;
        if (transition.phase === 'waiting-settle') runSettledAnimation(transition);
        else settleTransition(transition, false);
      });
    } else if (transition.phase === 'drag') {
      updateDragPosition(transition, transition.offset);
    }
  }, [location.key, location.pathname, location.search]);

  useEffect(
    () => () => {
      cancelFrame();
      if (timeoutRef.current) window.clearTimeout(timeoutRef.current);
      transitionRef.current?.ghost.remove();
      pageRef.current?.style.removeProperty('transform');
      pageRef.current?.style.removeProperty('transition');
      pageRef.current?.classList.remove('mobile-transition-active');
    },
    [],
  );

  const handleTouchStart = (event) => {
    if (transitionRef.current) return;
    if (
      role !== 'STUDENT' ||
      !window.matchMedia('(max-width: 760px)').matches ||
      event.touches.length !== 1 ||
      isSwipeExcludedTarget(event.target)
    ) {
      touchStart.current = null;
      return;
    }
    const { clientX, clientY } = event.touches[0];
    touchStart.current = { clientX, clientY, mode: 'pending', origin: location.pathname };
  };

  const handleTouchMove = (event) => {
    const gesture = touchStart.current;
    if (!gesture) return;
    if (event.touches.length !== 1) {
      touchStart.current = null;
      if (gesture.mode === 'drag' && gesture.transition) settleTransition(gesture.transition, true);
      return;
    }
    const touch = event.touches[0];
    const deltaX = touch.clientX - gesture.clientX;
    const deltaY = touch.clientY - gesture.clientY;

    if (gesture.mode === 'pending') {
      if (Math.abs(deltaX) < 12 && Math.abs(deltaY) < 12) return;
      if (Math.abs(deltaY) >= Math.abs(deltaX) * 1.15) {
        gesture.mode = 'vertical';
        return;
      }
      if (Math.abs(deltaX) < Math.abs(deltaY) * 1.25) return;

      const direction = deltaX < 0 ? 1 : -1;
      const destination = getStudentSwipeDestination(gesture.origin, -direction * 100, 0, 1);
      if (!destination) {
        gesture.mode = 'edge';
        return;
      }
      if (reduceMotion()) {
        gesture.mode = 'reduced-motion';
        gesture.destination = destination;
        gesture.direction = direction;
        return;
      }
      const started = startTransition(destination, direction, 'drag', deltaX);
      if (started) {
        gesture.mode = 'drag';
        gesture.transition = transitionRef.current;
      }
    }

    if (gesture.mode === 'drag' && gesture.transition && transitionRef.current === gesture.transition) {
      gesture.transition.offset = deltaX;
      if (frameRef.current === null) {
        frameRef.current = requestAnimationFrame(() => {
          frameRef.current = null;
          if (transitionRef.current !== gesture.transition || gesture.transition.phase !== 'drag') return;
          updateDragPosition(gesture.transition, gesture.transition.offset);
        });
      }
    }
  };

  const handleTouchEnd = (event) => {
    const gesture = touchStart.current;
    touchStart.current = null;
    if (!gesture) return;
    if (
      event.changedTouches.length !== 1 ||
      event.touches.length > 0 ||
      (gesture.mode !== 'drag' && isSwipeExcludedTarget(event.target))
    ) {
      if (gesture.mode === 'drag' && gesture.transition) settleTransition(gesture.transition, true);
      return;
    }
    const touch = event.changedTouches[0];
    const deltaX = touch.clientX - gesture.clientX;
    const deltaY = touch.clientY - gesture.clientY;
    if (gesture.mode === 'reduced-motion') {
      const destination = getStudentSwipeDestination(gesture.origin, deltaX, deltaY);
      if (destination) navigate(destination);
    } else if (gesture.mode === 'drag' && gesture.transition) {
      cancelFrame();
      const threshold = Math.max(60, gesture.transition.width * 0.22);
      const towardDestination = Math.sign(deltaX) === -gesture.transition.direction;
      const commit = towardDestination
        && Math.abs(deltaX) >= threshold
        && Math.abs(deltaX) > Math.abs(deltaY) * 1.25;
      settleTransition(gesture.transition, !commit);
    }
  };

  const handleTouchCancel = () => {
    const gesture = touchStart.current;
    touchStart.current = null;
    if (gesture?.mode === 'drag' && gesture.transition) {
      cancelFrame();
      settleTransition(gesture.transition, true);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="app-shell">
      <Sidebar
        role={role}
        open={menuOpen}
        onClose={() => setMenuOpen(false)}
        onLogout={handleLogout}
        location={location}
      />
      <div className="app-main">
        <Topbar user={user} onMenu={() => setMenuOpen(true)} />
        <main
          className="app-content"
          id="main"
          onTouchStart={handleTouchStart}
          onTouchMove={handleTouchMove}
          onTouchEnd={handleTouchEnd}
          onTouchCancel={handleTouchCancel}
        >
          <div className="mobile-transition-stage" ref={stageRef}>
            <div className="mobile-transition-page" ref={pageRef}>
              <Outlet />
            </div>
          </div>
        </main>
      </div>
      <MobileBottomNavigation role={role} onNavigate={handleBottomNavigation} />
      <MobileQuickActions role={role} />
    </div>
  );
}
