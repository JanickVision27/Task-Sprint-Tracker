import { useState } from 'react';
import Modal from './Modal';

const ROLES = [
  {
    role: 'ADMIN',
    title: 'Admin — Full Workspace Access',
    badgeClass: 'bg-purple-50 text-purple-700 border-purple-200',
    canDo: [
      'Create, edit, and delete any Project',
      'Create, edit, and delete any Sprint',
      'Create, assign, move, and delete any Task on any board',
    ],
  },
  {
    role: 'MANAGER',
    title: 'Manager — Sprint & Team Lead',
    badgeClass: 'bg-indigo-50 text-indigo-700 border-indigo-200',
    canDo: [
      'Create, update, and delete Projects and Sprints',
      'Create tasks and assign them to team members',
      'Move or update any task across To Do, In Progress, and Done',
    ],
  },
  {
    role: 'MEMBER',
    title: 'Member — Individual Contributor',
    badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    canDo: [
      'View all Projects, Sprints, and Kanban boards',
      'Create new Tasks and claim unassigned tasks (+ Assign to me)',
      'Move and update ONLY tasks assigned to themselves',
      'Cannot create/delete Sprints or move another teammate’s task',
    ],
  },
];

export default function RoleGuideButton({ compact = false }) {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <>
      <button
        type="button"
        onClick={() => setIsOpen(true)}
        title="View Role Permissions & Board Rules"
        className={
          compact
            ? 'inline-flex items-center gap-1 text-xs font-medium text-indigo-600 hover:text-indigo-800 cursor-pointer'
            : 'inline-flex items-center gap-1.5 text-xs font-medium text-slate-600 hover:text-indigo-600 bg-slate-100 hover:bg-indigo-50 border border-slate-200 hover:border-indigo-200 px-2.5 py-1.5 rounded-lg transition cursor-pointer'
        }
      >
        <span className="w-4 h-4 rounded-full bg-indigo-600 text-white text-[10px] font-bold inline-flex items-center justify-center leading-none">
          ?
        </span>
        <span>Role Guide</span>
      </button>

      <Modal
        isOpen={isOpen}
        onClose={() => setIsOpen(false)}
        title="Role Permissions & Board Rules"
      >
        <div className="space-y-4">
          <p className="text-xs text-slate-500">
            How permissions work for each role in <strong>Task &amp; Sprint Tracker</strong>:
          </p>

          {ROLES.map((item) => (
            <div
              key={item.role}
              className="p-3.5 rounded-xl border border-slate-200 bg-slate-50/60"
            >
              <div className="flex items-center gap-2 mb-2">
                <span
                  className={`text-[11px] font-bold px-2 py-0.5 rounded-full border ${item.badgeClass}`}
                >
                  {item.role}
                </span>
                <span className="text-sm font-semibold text-slate-800">{item.title}</span>
              </div>
              <ul className="text-xs text-slate-600 space-y-1 pl-4 list-disc">
                {item.canDo.map((point) => (
                  <li key={point}>{point}</li>
                ))}
              </ul>
            </div>
          ))}

          <div className="p-3.5 rounded-xl border border-amber-200 bg-amber-50/70">
            <h4 className="text-xs font-semibold text-amber-900 mb-1">
              Active Business Rules
            </h4>
            <ul className="text-xs text-amber-800 space-y-1 pl-4 list-disc">
              <li>
                <strong>Assignee Required for DONE:</strong> A task cannot be moved to{' '}
                <strong>Done</strong> unless it is assigned to someone.
              </li>
              <li>
                <strong>Sprint Dates:</strong> A sprint’s end date cannot be earlier than its
                start date.
              </li>
            </ul>
          </div>

          <div className="flex justify-end pt-1">
            <button
              type="button"
              onClick={() => setIsOpen(false)}
              className="px-4 py-2 text-xs font-medium bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition cursor-pointer"
            >
              Got it
            </button>
          </div>
        </div>
      </Modal>
    </>
  );
}
