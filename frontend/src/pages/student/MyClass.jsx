import { useState } from 'react';
import ClassCard from '../../components/cards/ClassCard';
import Card from '../../components/common/Card';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import PageHeader from '../../components/common/PageHeader';
import JoinClassForm from '../../components/forms/JoinClassForm';
import LeaveClassDialog from '../../components/forms/LeaveClassDialog';
import { useApi } from '../../hooks/useApi';
import { useToast } from '../../hooks/useToast';
import { studentService } from '../../services/studentService';

export default function MyClass() {
  const { data, setData, loading, error, reload } = useApi(studentService.classes);
  const [leaving, setLeaving] = useState(null);
  const toast = useToast();

  const onLeft = (cls) => {
    setLeaving(null);
    setData((current) => current.filter((c) => c.id !== cls.id));
    reload();
  };

  const onJoined = (result) => {
    toast.success(result.message);
    reload();
  };

  return (
    <>
      <PageHeader title="My class" subtitle="Join a class with the code your teacher gives you" />

      <Card title="Join a class" subtitle="Class codes look like DOLPHIN-A8F21">
        <JoinClassForm onJoined={onJoined} />
      </Card>

      <Card title="Joined classes">
        {loading ? (
          <LoadingState label="Loading classes…" />
        ) : error ? (
          <ErrorState title="Unable to load classes" message={error.message} onRetry={reload} />
        ) : data.length === 0 ? (
          <EmptyState icon="book" title="You haven't joined a class yet." message="Ask your teacher for the class code and enter it above." />
        ) : (
          <div className="card-grid">
            {data.map((cls) => (
              <ClassCard key={cls.id} cls={cls} onLeave={setLeaving} />
            ))}
          </div>
        )}
      </Card>

      <LeaveClassDialog cls={leaving} onCancel={() => setLeaving(null)} onLeft={onLeft} />
    </>
  );
}
