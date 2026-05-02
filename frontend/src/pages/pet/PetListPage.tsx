import { useEffect, useState } from 'react';
import { Row, Col, Card, Button, Popconfirm, message, Empty, Avatar } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, UserOutlined } from '@ant-design/icons';
import { getPets, createPet, updatePet, deletePet } from '@/api/pet';
import type { PetVO, PetCreateRequest } from '@/types';
import { PetTypeMap, GenderMap } from '@/utils/constants';
import { formatAge } from '@/utils/format';
import PetFormModal from './PetFormModal';
import PageLoading from '@/components/PageLoading';

export default function PetListPage() {
  const [pets, setPets] = useState<PetVO[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingPet, setEditingPet] = useState<PetVO | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const fetchPets = async () => {
    setLoading(true);
    try {
      const data = await getPets();
      setPets(data);
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPets();
  }, []);

  const handleAdd = () => {
    setEditingPet(null);
    setModalOpen(true);
  };

  const handleEdit = (pet: PetVO) => {
    setEditingPet(pet);
    setModalOpen(true);
  };

  const handleSubmit = async (values: PetCreateRequest) => {
    setSubmitting(true);
    try {
      if (editingPet) {
        await updatePet(editingPet.id, values);
        message.success('更新成功');
      } else {
        await createPet(values);
        message.success('添加成功');
      }
      setModalOpen(false);
      fetchPets();
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deletePet(id);
      message.success('删除成功');
      fetchPets();
    } catch (err: any) {
      message.error(err.message);
    }
  };

  if (loading) return <PageLoading />;

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-semibold m-0">宠物管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          添加宠物
        </Button>
      </div>

      {pets.length === 0 ? (
        <Empty description="暂无宠物" />
      ) : (
        <Row gutter={[16, 16]}>
          {pets.map((pet) => (
            <Col key={pet.id} xs={24} sm={12} md={8} lg={6}>
              <Card
                hoverable
                actions={[
                  <EditOutlined key="edit" onClick={() => handleEdit(pet)} />,
                  <Popconfirm key="delete" title="确认删除此宠物？" onConfirm={() => handleDelete(pet.id)}>
                    <DeleteOutlined />
                  </Popconfirm>,
                ]}
              >
                <Card.Meta
                  avatar={
                    pet.avatarUrl ? (
                      <Avatar src={pet.avatarUrl} size={64} />
                    ) : (
                      <Avatar size={64} icon={<UserOutlined />} />
                    )
                  }
                  title={pet.petName}
                  description={
                    <div className="space-y-1 text-sm">
                      <div>类型: {PetTypeMap[pet.petType] || pet.petType}</div>
                      <div>年龄: {formatAge(pet.age)}</div>
                      <div>性别: {GenderMap[pet.gender] || pet.gender || '-'}</div>
                    </div>
                  }
                />
              </Card>
            </Col>
          ))}
        </Row>
      )}

      <PetFormModal
        open={modalOpen}
        pet={editingPet}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        loading={submitting}
      />
    </div>
  );
}
