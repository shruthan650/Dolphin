import { useState } from 'react';
import ClassCard from '../../components/cards/ClassCard';
import Button from '../../components/common/Button';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import Modal from '../../components/common/Modal';
import PageHeader from '../../components/common/PageHeader';
import ClassForm from '../../components/forms/ClassForm';
import { useApi } from '../../hooks/useApi';
import { useToast } from '../../hooks/useToast';
import { classService } from '../../services/classService';

export default function Classes() {
  const { data, setData, loading, error, reload } = useApi(classService.mine);
  const [creating, setCreating] = useState(false);
  const [newId, setNewId] = useState(null);
  const toast = useToast();

  const createClass = async (values) => {
    const created = await classService.create(values);
    setData((current) => [created, ...(current || [])]);
    setNewId(created.id);
    setCreating(false);
    toast.success(`Class created successfully. Share code ${created.classCode} with your students.`);
  };

  return (
    <>
      <PageHeader
        title="My classes"
        subtitle="Share a class code with students so they can join"
        actions={
          <Button icon="plus" onClick={() => setCreating(true)}>
            Create class
          </Button>
        }
      />

      {loading ? (
        <LoadingState label="Loading classes…" />
      ) : error ? (
        <ErrorState title="Unable to load classes" message={error.message} onRetry={reload} />
      ) : data.length === 0 ? (
        <EmptyState
          icon="book"
          title="You haven't created any classes yet."
          message="Create a class to get started."
          action={
            <Button icon="plus" onClick={() => setCreating(true)}>
              Create class
            </Button>
          }
        />
      ) : (
        <div className="card-grid">
          {data.map((cls) => (
            <ClassCard key={cls.id} cls={cls} to={`/teacher/classes/${cls.id}`} highlight={cls.id === newId} />
          ))}
        </div>
      )}

      <Modal open={creating} title="Create class" description="A unique class code is generated automatically." onClose={() => setCreating(false)}>
        <ClassForm onSubmit={createClass} onCancel={() => setCreating(false)} />
      </Modal>
    </>
  );
}
