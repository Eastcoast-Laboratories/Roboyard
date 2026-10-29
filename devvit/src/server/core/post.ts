import { reddit } from '@devvit/web/server';

export const createPost = async () => {
  return await reddit.submitCustomPost({
    title: 'Arcane Wisps: Slide. Ricochet. Return.',
  });
};
