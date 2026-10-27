export function canViewLeetCodeStreak(viewerRole, profileContext) {
  return (
    (viewerRole === 'STUDENT' && profileContext === 'own-student-profile') ||
    (viewerRole === 'TEACHER' && profileContext === 'authorized-student-profile')
  );
}
